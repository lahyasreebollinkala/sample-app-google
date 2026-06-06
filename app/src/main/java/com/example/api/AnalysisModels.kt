package com.example.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DecisionAnalysis(
    val verdict: String,               // AI's clear recommendation/ratio for the tiebreaker
    val summary: String,               // Quick description of why this recommendation was reached
    val prosCons: ProsConsData,         // Pros and Cons for options
    val comparisonTable: ComparisonTableData, // Side-by-side comparison matrix
    val swotAnalysis: SwotData          // General SWOT analysis or SWOT of leading option
)

@JsonClass(generateAdapter = true)
data class ProsConsData(
    val optionsList: List<OptionProsCons>
)

@JsonClass(generateAdapter = true)
data class OptionProsCons(
    val optionName: String,
    val pros: List<String>,
    val cons: List<String>
)

@JsonClass(generateAdapter = true)
data class ComparisonTableData(
    val headers: List<String>,          // e.g., ["Criteria", "Option A", "Option B"]
    val rows: List<ComparisonRow>
)

@JsonClass(generateAdapter = true)
data class ComparisonRow(
    val criteriaName: String,
    val values: List<String>            // Corresponding values for columns matching headers (starting after Criteria)
)

@JsonClass(generateAdapter = true)
data class SwotData(
    val strengths: List<String>,
    val weaknesses: List<String>,
    val opportunities: List<String>,
    val threats: List<String>
)
