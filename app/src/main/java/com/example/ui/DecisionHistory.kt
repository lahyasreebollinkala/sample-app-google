package com.example.ui

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.DecisionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionHistory(
    decisions: List<DecisionEntity>,
    searchQuery: String,
    onSearchChanged: (String) -> Unit,
    showFavoritesOnly: Boolean,
    onToggleFavoritesFilter: () -> Unit,
    onSelect: (DecisionEntity) -> Unit,
    onFavoriteToggle: (DecisionEntity) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Decision Register",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            
            Text(
                text = "${decisions.size} item(s)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search and Pinned Filter Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("search_decisions_input"),
                placeholder = { Text("Search choices...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Close, "Clear search", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Favorites quick toggle button
            FilterChip(
                selected = showFavoritesOnly,
                onClick = onToggleFavoritesFilter,
                modifier = Modifier
                    .height(44.dp)
                    .testTag("favorites_filter_chip"),
                label = { Text("Pinned") },
                leadingIcon = {
                    Icon(
                        imageVector = if (showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Pinned selection",
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (decisions.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (showFavoritesOnly) Icons.Default.Star else Icons.Default.List,
                        contentDescription = "Empty History",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (showFavoritesOnly) "No pinned decisions found" else "Decision Register is empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (showFavoritesOnly) "Mark a dilemma as a favorite to pin it here." else "Describe a complex decision above to analyze details and keep records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp),
                        onTextLayout = {}
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(decisions, key = { it.id }) { decision ->
                    DecisionHistoryCard(
                        decision = decision,
                        onClick = { onSelect(decision) },
                        onFavoriteToggle = { onFavoriteToggle(decision) },
                        onDelete = { onDelete(decision.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DecisionHistoryCard(
    decision: DecisionEntity,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        decision.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    val badgeLabel = when (decision.analysisType) {
        "ALL" -> "Full Suite"
        "PROS_CONS" -> "Pros & Cons"
        "COMPARISON" -> "Comparison"
        "SWOT" -> "SWOT Focus"
        else -> "Custom"
    }

    val badgeColor = when (decision.analysisType) {
        "ALL" -> MaterialTheme.colorScheme.primaryContainer
        "PROS_CONS" -> MaterialTheme.colorScheme.tertiaryContainer
        "COMPARISON" -> MaterialTheme.colorScheme.secondaryContainer
        "SWOT" -> Color(0xFFFFF3CD) // Light warm SWOT color
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val badgeTextColor = when (decision.analysisType) {
        "ALL" -> MaterialTheme.colorScheme.onPrimaryContainer
        "PROS_CONS" -> MaterialTheme.colorScheme.onTertiaryContainer
        "COMPARISON" -> MaterialTheme.colorScheme.onSecondaryContainer
        "SWOT" -> Color(0xFF856404)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("decision_history_card_${decision.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = badgeColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = decision.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (decision.context.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = decision.context,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Items Column
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Favorite Button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("favorite_button_${decision.id}")
                ) {
                    Icon(
                        imageVector = if (decision.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite Toggle",
                        tint = if (decision.isFavorite) Color.Red else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_button_${decision.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
