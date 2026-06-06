package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DecisionForm(
    onGenerate: (question: String, context: String, type: String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var question by remember { mutableStateOf("") }
    var context by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ALL") }

    val analysisTypes = listOf(
        "ALL" to "Complete Suite (Verdict + Pros/Cons + Table + SWOT)",
        "PROS_CONS" to "Pros & Cons Focus",
        "COMPARISON" to "Comparison Matrix Focus",
        "SWOT" to "SWOT Analysis Focus"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Consult the Tiebreaker",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Explain the options or trade-offs you're weighing. The AI will build a thorough, balanced breakdown & recommend the winning path.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("What decision are you trying to make?") },
                placeholder = { Text("e.g. Should I buy a Tesla Model Y or a Toyota RAV4 Hybrid?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("question_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3,
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = context,
                onValueChange = { context = it },
                label = { Text("Context & Constraints (Optional)") },
                placeholder = { Text("e.g. Budget is $45k, daily commute is 40 miles, plan to keep the car for 8 years...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("context_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4,
                enabled = !isLoading,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Analysis Focus Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Selector Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(vertical = 8.dp)
            ) {
                analysisTypes.forEach { (typeKey, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedType == typeKey),
                            onClick = { if (!isLoading) selectedType = typeKey },
                            modifier = Modifier.testTag("radio_button_$typeKey")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedType == typeKey) FontWeight.Medium else FontWeight.Normal,
                            color = if (selectedType == typeKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Analyzing options & finding tiebreakers...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Button(
                    onClick = {
                        if (question.isNotBlank()) {
                            onGenerate(question, context, selectedType)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("generate_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = question.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send to AI"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Generate Structured Analysis",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Simple note card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Tip Info",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tip: Make your options descriptive. For example, 'study design vs coding' returns deeper criteria matching than 'school vs job'.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
