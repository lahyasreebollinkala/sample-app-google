package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.db.DecisionEntity
import com.example.ui.DecisionDetail
import com.example.ui.DecisionForm
import com.example.ui.DecisionHistory
import com.example.ui.DecisionViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DecisionViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val selectedDecision by viewModel.selectedDecision.collectAsStateWithLifecycle()
                val decisions by viewModel.decisions.collectAsStateWithLifecycle()
                val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
                val error by viewModel.error.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()

                // Check if API Key is configured in build system
                val isApiKeyConfigured = remember {
                    BuildConfig.GEMINI_API_KEY.isNotEmpty() && 
                    BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
                }

                if (currentScreen == "DETAIL" && selectedDecision != null) {
                    DecisionDetail(
                        decision = selectedDecision!!,
                        onBack = { viewModel.goBackToHome() },
                        onFavoriteToggle = { viewModel.toggleFavorite(selectedDecision!!) },
                        onDelete = { viewModel.deleteDecision(selectedDecision!!.id) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Scaffold(
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "⚖️ The Tiebreaker",
                                            fontWeight = FontWeight.ExtraBold,
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            // High level warning banners if credentials are not configured but do not block app use
                            if (!isApiKeyConfigured) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .testTag("api_key_warning_banner"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Missing Key",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Prototyping Key Required",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = "To request AI's help, please insert your GEMINI_API_KEY securely into the Secrets panel in Google AI Studio. It will automatically inject without hardcoding local files.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Dynamic state-based error banner
                            if (error != null) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .testTag("analysis_error_banner"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Dilemma Analysis Failed",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = error ?: "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.clearError() },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Dismiss error",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Left layout for interactive form with database history listed below
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                // Scrollable elements comprising the input form and dynamic tip panel
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    DecisionForm(
                                        onGenerate = { question, context, type ->
                                            viewModel.generateDecisionAnalysis(question, context, type)
                                        },
                                        isLoading = isLoading,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Interactive Database History panel
                                DecisionHistory(
                                    decisions = decisions,
                                    searchQuery = searchQuery,
                                    onSearchChanged = { viewModel.setSearchQuery(it) },
                                    showFavoritesOnly = showFavoritesOnly,
                                    onToggleFavoritesFilter = { viewModel.toggleFavoritesFilter() },
                                    onSelect = { viewModel.selectDecision(it) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onDelete = { viewModel.deleteDecision(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
