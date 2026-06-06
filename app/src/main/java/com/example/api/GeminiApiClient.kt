package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    private val SYSTEM_INSTRUCTION = """
        You are "The Tiebreaker", a brilliant, analytical, and highly structured decision-making assistant.
        Your goal is to parse the user's decision query (and optional supporting context) and analyze the options to help them resolve their dilemma.
        
        You must structure your response EXACTLY as the requested JSON schema.
        
        Analyze the options with great care:
        1. Identify the options: If the user provides explicit options, use them. If they provide a single action ("Should I resign?"), the options must be: "Option A: Yes, resign" and "Option B: No, stay".
        2. Generate a clear "verdict" which represents "The Tiebreaker's choice" based on weights of arguments. Your verdict must be a balanced, clear determination, stating which option is recommended and a direct reason why.
        3. Formulate of 3-5 pros and 3-5 cons for each of the options.
        4. Develop a rigorous comparison table comparing the identified options side-by-side. You must define 4-5 relevant criteria headers, and supply a summary value of each option under each criteria. Keep column count equal to option count + 1.
        5. Formulate a comprehensive SWOT Analysis for the situation or the recommended option, featuring 3-4 lists under Strengths, Weaknesses, Opportunities, and Threats.
        
        Verify that you return valid, parsable JSON matching this schema:
        {
          "verdict": "string (Recommendation: option name and primary reason)",
          "summary": "string (A narrative summary of the dilemma and core trade-offs)",
          "prosCons": {
            "optionsList": [
              {
                "optionName": "string",
                "pros": ["string"],
                "cons": ["string"]
              }
            ]
          },
          "comparisonTable": {
            "headers": ["string (e.g. Criteria, Option A, Option B)"],
            "rows": [
              {
                "criteriaName": "string (e.g. Initial Cost)",
                "values": ["string (for Option A)", "string (for Option B)"]
              }
            ]
          },
          "swotAnalysis": {
            "strengths": ["string"],
            "weaknesses": ["string"],
            "opportunities": ["string"],
            "threats": ["string"]
          }
        }
        
        Keep all texts punchy, insightful, and practical. Return ONLY valid JSON. No markdown wrappings (like ```json).
    """.trimIndent()

    suspend fun analyzeDecision(
        question: String,
        context: String,
        type: String // "ALL", "PROS_CONS", "COMPARISON", "SWOT" (the prompt can format focal elements)
    ): String? {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API key is missing or is placeholder!")
            return null
        }

        // Adjust the user prompt based on what analysis type the user requested, 
        // but always ask for the full structured JSON so we can populate all details beautifully.
        val userPrompt = """
            Make a decision analysis for:
            Topic: $question
            Supporting Context: ${if (context.isBlank()) "None provided" else context}
            Focus Mode: $type
            
            Please complete the full JSON structure. Even if the focus is $type, fill in the rest of the fields with great quality so I can inspect other views as well.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = userPrompt)
                    )
                )
            ),
            generationConfig = GenerationConfig(), // uses default application/json config
            systemInstruction = Content(
                parts = listOf(
                    Part(text = SYSTEM_INSTRUCTION)
                )
            )
        )

        return try {
            val response = apiService.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d(TAG, "Raw response text: ${jsonText ?: "null"}")
            jsonText
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            null
        }
    }

    /**
     * Parses the JSON returned from Gemini back into a DecisionAnalysis object.
     */
    fun parseAnalysis(json: String): DecisionAnalysis? {
        return try {
            val adapter = moshi.adapter(DecisionAnalysis::class.java)
            // Sometimes Gemini puts markdown around the code despite instruction. Clean it.
            var cleaned = json.trim()
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.substringAfter("\n")
                if (cleaned.endsWith("```")) {
                    cleaned = cleaned.substringBeforeLast("```")
                }
                cleaned = cleaned.trim()
                if (cleaned.startsWith("json")) {
                    cleaned = cleaned.substring(4).trim()
                }
            }
            adapter.fromJson(cleaned)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse analysis JSON: ${e.message}", e)
            null
        }
    }
}
