package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.DecisionEntity
import com.example.api.GeminiApiClient
import com.example.api.DecisionAnalysis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionDetail(
    decision: DecisionEntity,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Parse the JSON result
    val analysis = remember(decision.jsonResult) {
        GeminiApiClient.parseAnalysis(decision.jsonResult)
    }

    // Initialize Simulation Factors state list maintained across tab changes
    val simulationFactors = remember(analysis) {
        val list = mutableStateListOf<SimulatedFactor>()
        analysis?.prosCons?.optionsList?.forEach { option ->
            option.pros.forEachIndexed { index, pro ->
                list.add(
                    SimulatedFactor(
                        id = "${option.optionName}_pro_${index}",
                        isPro = true,
                        text = pro,
                        optionName = option.optionName,
                        weight = mutableStateOf(2f)
                    )
                )
            }
            option.cons.forEachIndexed { index, con ->
                list.add(
                    SimulatedFactor(
                        id = "${option.optionName}_con_${index}",
                        isPro = false,
                        text = con,
                        optionName = option.optionName,
                        weight = mutableStateOf(2f)
                    )
                )
            }
        }
        list
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("🎯 Verdict", "✅ Pros & Cons", "⚖️ Comparison", "⚡ SWOT", "🎛️ Simulation")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Decision Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    IconButton(onClick = onFavoriteToggle, modifier = Modifier.testTag("detail_favorite_button")) {
                        Icon(
                            imageVector = if (decision.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (decision.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("detail_delete_button")) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete dilemma",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Main dilemma Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "THE DILEMMA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = decision.question,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (decision.context.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = decision.context,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (analysis == null) {
                // Formatting or parsing error backup UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error parsing",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Parsing Misalignment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The AI created a detailed analysis, but the raw output did not completely align with our layout structures. Below is the full text response for your reference:",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = decision.jsonResult,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            } else {
                // Secondary navigation tab row
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_tab_row"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = (selectedTab == index),
                            onClick = { selectedTab = index },
                            text = { Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.testTag("detail_tab_button_$index")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> VerdictView(analysis)
                        1 -> ProsConsView(analysis)
                        2 -> ComparisonView(analysis)
                        3 -> SwotView(analysis)
                        4 -> SimulationView(analysis, simulationFactors)
                    }
                }
            }
        }
    }
}

@Composable
fun VerdictView(analysis: DecisionAnalysis) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Contrast Verdict Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verdict",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "THE TIEBREAKER VERDICT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = analysis.verdict,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp
                )
            }
        }

        // Summary Breakdown section
        Text(
            text = "EXECUTIVE SUMMARY",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = analysis.summary,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                fontStyle = FontStyle.Normal,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Help quote/affirmation
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Tip",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "This advice represents an optimized assessment based on logical analysis. Check other tabs above to compare individual parameters side by side.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProsConsView(analysis: DecisionAnalysis) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        analysis.prosCons.optionsList.forEach { option ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = option.optionName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pros list
                    Text(
                        text = "PROS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32), // Custom green
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (option.pros.isEmpty()) {
                        Text(
                            text = "• No significant pros identified.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        option.pros.forEach { pro ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "✓",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    text = pro,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cons list
                    Text(
                        text = "CONS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (option.cons.isEmpty()) {
                        Text(
                            text = "• No significant cons identified.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        option.cons.forEach { con ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "✗",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    text = con,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonView(analysis: DecisionAnalysis) {
    val tableData = analysis.comparisonTable
    
    if (tableData.headers.isEmpty() || tableData.rows.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No side-by-side comparison matrix generated.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "COMPARISON GRID",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            // Horizontally Scrollable wrapper in case columns are wide
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tableData.headers.forEachIndexed { colIndex, header ->
                            Text(
                                text = header,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (colIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .width(if (colIndex == 0) 120.dp else 160.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rows
                    tableData.rows.forEachIndexed { rowIndex, row ->
                        val itemBg = if (rowIndex % 2 == 0) {
                            Color.Transparent
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = itemBg, shape = RoundedCornerShape(4.dp))
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Criteria Name
                            Text(
                                text = row.criteriaName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .width(120.dp)
                                    .padding(end = 8.dp)
                            )

                            // Criteria Options value cells
                            row.values.forEachIndexed { valIndex, cellValue ->
                                Text(
                                    text = cellValue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .width(160.dp)
                                        .padding(end = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SwotView(analysis: DecisionAnalysis) {
    val swot = analysis.swotAnalysis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "STRATEGIC SWOT ANALYSIS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        // 1. Strengths (Purple/Indigo Theme)
        SwotSectionCard(
            title = "STRENGTHS",
            items = swot.strengths,
            bulletChar = "▲",
            cardColor = Color(0xFFE8DEF8),
            titleColor = Color(0xFF21005D),
            textColor = Color(0xFF21005D)
        )

        // 2. Weaknesses (Amber Theme)
        SwotSectionCard(
            title = "WEAKNESSES",
            items = swot.weaknesses,
            bulletChar = "▼",
            cardColor = Color(0xFFFFE082).copy(alpha = 0.2f),
            titleColor = Color(0xFF705000),
            textColor = Color(0xFF705000)
        )

        // 3. Opportunities (Blue Theme)
        SwotSectionCard(
            title = "OPPORTUNITIES",
            items = swot.opportunities,
            bulletChar = "⬥",
            cardColor = Color(0xFFC2E7FF).copy(alpha = 0.3f),
            titleColor = Color(0xFF004A77),
            textColor = Color(0xFF004A77)
        )

        // 4. Threats (Red Theme)
        SwotSectionCard(
            title = "THREATS",
            items = swot.threats,
            bulletChar = "⚠",
            cardColor = Color(0xFFF9DEDC),
            titleColor = Color(0xFF410E0B),
            textColor = Color(0xFF410E0B)
        )
    }
}

@Composable
fun SwotSectionCard(
    title: String,
    items: List<String>,
    bulletChar: String,
    cardColor: Color,
    titleColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (items.isEmpty()) {
                Text(
                    text = "No critical points analyzed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.5f)
                )
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = bulletChar,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = titleColor,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

data class SimulatedFactor(
    val id: String,
    val isPro: Boolean,
    val text: String,
    val optionName: String,
    val weight: MutableState<Float>,
    val isMuted: MutableState<Boolean> = mutableStateOf(false),
    val isCustom: Boolean = false
)

@Composable
fun LiveDecisionGauge(percentage: Float, optionAName: String, optionBName: String) {
    val animatedAngle by animateFloatAsState(
        targetValue = 180f - (180f * percentage), // Swings logically between 180f (Option B) and 0f (Option A)
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "NeedleAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp, 130.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outline
            val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val center = Offset(width / 2f, height)
                val outerRadius = width / 2f - 20.dp.toPx()
                val innerRadius = outerRadius - 16.dp.toPx()

                // Draw background arc
                drawArc(
                    color = surfaceVariant,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    size = Size(outerRadius * 2f, outerRadius * 2f),
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    style = Stroke(width = 16.dp.toPx())
                )

                // Left 40% (Option B preference) - Red/Warm tint
                drawArc(
                    color = Color(0xFFF9DEDC),
                    startAngle = 180f,
                    sweepAngle = 72f,
                    useCenter = false,
                    size = Size(outerRadius * 2f, outerRadius * 2f),
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    style = Stroke(width = 16.dp.toPx())
                )

                // Mid 20% (Tie range) - Yellow
                drawArc(
                    color = Color(0xFFFFE082),
                    startAngle = 252f,
                    sweepAngle = 36f,
                    useCenter = false,
                    size = Size(outerRadius * 2f, outerRadius * 2f),
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    style = Stroke(width = 16.dp.toPx())
                )

                // Right 40% (Option A preference) - Green/Indigo
                drawArc(
                    color = Color(0xFFE8F5E9),
                    startAngle = 288f,
                    sweepAngle = 72f,
                    useCenter = false,
                    size = Size(outerRadius * 2f, outerRadius * 2f),
                    topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                    style = Stroke(width = 16.dp.toPx())
                )

                // Tick marks
                val tickCount = 11
                for (i in 0 until tickCount) {
                    val angleRad = (180f + (i * 18f)) * (Math.PI / 180f)
                    val startX = center.x + (innerRadius - 6.dp.toPx()) * Math.cos(angleRad).toFloat()
                    val startY = center.y + (innerRadius - 6.dp.toPx()) * Math.sin(angleRad).toFloat()
                    val endX = center.x + innerRadius * Math.cos(angleRad).toFloat()
                    val endY = center.y + innerRadius * Math.sin(angleRad).toFloat()
                    drawLine(
                        color = outlineColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Center pivot cap shadow and fill
                drawCircle(
                    color = primaryColor,
                    radius = 16.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 6.dp.toPx(),
                    center = center
                )

                // Needle logic
                val needleAngleRad = animatedAngle * (Math.PI / 180f)
                val needleLength = outerRadius - 4.dp.toPx()
                val needleEndX = center.x + needleLength * Math.cos(needleAngleRad).toFloat()
                val needleEndY = center.y + needleLength * Math.sin(needleAngleRad).toFloat()

                // Draw the needle pointing out
                drawLine(
                    color = primaryColor,
                    start = center,
                    end = Offset(needleEndX, needleEndY),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Left and Right Option indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = optionBName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
                Text(
                    text = "${((1f - percentage) * 100f).toInt()}% Strength",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Center status Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = when {
                        percentage > 0.55f -> "Favoring Left Option"
                        percentage < 0.45f -> "Favoring Right Option"
                        else -> "Perfect Balance"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = optionAName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "${(percentage * 100f).toInt()}% Strength",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationView(
    analysis: DecisionAnalysis,
    simulationFactors: MutableList<SimulatedFactor>
) {
    val optionAName = remember(analysis) {
        analysis.prosCons.optionsList.getOrNull(0)?.optionName ?: "Option A"
    }
    val optionBName = remember(analysis) {
        if (analysis.prosCons.optionsList.size >= 2) {
            analysis.prosCons.optionsList[1].optionName
        } else {
            "Alternative"
        }
    }

    var newFactorText by remember { mutableStateOf("") }
    var isNewFactorPro by remember { mutableStateOf(true) }
    var selectedOptionTarget by remember { mutableStateOf(optionAName) }
    var showAddForm by remember { mutableStateOf(false) }

    // Math: Sum options
    val activeFactors = simulationFactors.filter { !it.isMuted.value }
    val supportA = activeFactors.filter {
        (it.optionName == optionAName && it.isPro) || (it.optionName == optionBName && !it.isPro)
    }.sumOf { it.weight.value.toDouble() }.toFloat()

    val supportB = activeFactors.filter {
        (it.optionName == optionAName && !it.isPro) || (it.optionName == optionBName && it.isPro)
    }.sumOf { it.weight.value.toDouble() }.toFloat()

    val totalSupport = supportA + supportB
    val percentage = if (totalSupport > 0f) supportA / totalSupport else 0.5f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Card containing live gauge and title
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TIEBREAKER SIMULATOR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Adjust weights of pros & cons or inject custom user assumptions below to see how the decision balance shifts in real time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                LiveDecisionGauge(
                    percentage = percentage,
                    optionAName = optionAName,
                    optionBName = optionBName
                )
            }
        }

        // Add Custom Factor Trigger / Expand Form Button
        if (!showAddForm) {
            Button(
                onClick = { showAddForm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("expand_simulation_form"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add factor")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Inject Custom Factor Into Math")
            }
        } else {
            // Elegant Factor Creator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NEW FACTOR STATEMENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newFactorText,
                        onValueChange = { newFactorText = it },
                        placeholder = { Text("e.g. My family is highly supportive of this...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_factor_text"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "CLASSIFY FACTOR",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pro vs Con segmented toggle
                        FilterChip(
                            selected = isNewFactorPro,
                            onClick = { isNewFactorPro = true },
                            label = { Text("Is a Pro (Advantage)") },
                            leadingIcon = if (isNewFactorPro) {
                                { Icon(Icons.Default.Check, contentDescription = "Pro") }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isNewFactorPro,
                            onClick = { isNewFactorPro = false },
                            label = { Text("Is a Con (Risk)") },
                            leadingIcon = if (!isNewFactorPro) {
                                { Icon(Icons.Default.Clear, contentDescription = "Con") }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Option:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))

                        // Chip select option target
                        FilterChip(
                            selected = selectedOptionTarget == optionAName,
                            onClick = { selectedOptionTarget = optionAName },
                            label = { Text(optionAName.take(15) + if (optionAName.length > 15) ".." else "") }
                        )

                        if (optionBName != "Alternative" || (analysis.prosCons.optionsList.size >= 2)) {
                            FilterChip(
                                selected = selectedOptionTarget == optionBName,
                                onClick = { selectedOptionTarget = optionBName },
                                label = { Text(optionBName.take(15) + if (optionBName.length > 15) ".." else "") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showAddForm = false
                                newFactorText = ""
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (newFactorText.isNotBlank()) {
                                    val customId = "custom_${System.currentTimeMillis()}"
                                    simulationFactors.add(
                                        SimulatedFactor(
                                            id = customId,
                                            isPro = isNewFactorPro,
                                            text = newFactorText,
                                            optionName = selectedOptionTarget,
                                            weight = mutableStateOf(3f), // Starts at heavy initial influence
                                            isCustom = true
                                        )
                                    )
                                    newFactorText = ""
                                    showAddForm = false
                                }
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("submit_custom_factor"),
                            enabled = newFactorText.isNotBlank(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Inject Block")
                        }
                    }
                }
            }
        }

        // List of all simulated factors with weight sliders
        Text(
            text = "DECISION FACTORS WEIGHT BOARD",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.1.sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        simulationFactors.forEach { factor ->
            // Card representing factor weight control
            val factorColor = if (factor.isMuted.value) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else if (factor.isPro) {
                Color(0xFFE8F5E9) // soft beautiful green pro
            } else {
                Color(0xFFF9DEDC) // soft beautiful red con
            }

            val accentLabelColor = if (factor.isMuted.value) {
                MaterialTheme.colorScheme.outline
            } else if (factor.isPro) {
                Color(0xFF2E7D32)
            } else {
                Color(0xFFC62828)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("factor_card_${factor.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = factorColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge denoting Pro/Con + Option target
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (factor.isPro) "PRO" else "CON",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentLabelColor,
                                modifier = Modifier
                                    .background(
                                        color = if (factor.isPro) Color(0xFFE8F5E9) else Color(0xFFF9DEDC),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            Text(
                                text = "for ${factor.optionName.take(18) + if (factor.optionName.length > 18) ".." else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (factor.isCustom) {
                                Text(
                                    text = "USER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        // Mute & Delete controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active status switch
                            IconButton(
                                onClick = { factor.isMuted.value = !factor.isMuted.value },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (factor.isMuted.value) Icons.Default.AddCircle else Icons.Default.CheckCircle,
                                    contentDescription = "Toggle Mute",
                                    tint = if (factor.isMuted.value) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (factor.isCustom) {
                                IconButton(
                                    onClick = { simulationFactors.remove(factor) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete factor",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = factor.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (factor.isMuted.value) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (factor.isMuted.value) FontWeight.Normal else FontWeight.Medium
                    )

                    if (!factor.isMuted.value) {
                        Spacer(modifier = Modifier.height(8.dp))

                        val currentWeight = factor.weight.value
                        val weightText = when {
                            currentWeight <= 1.2f -> "Low Importance (1.0x)"
                            currentWeight <= 2.2f -> "Medium Importance (2.0x)"
                            currentWeight <= 3.2f -> "High Importance (3.0x)"
                            else -> "Critical Importance (4.0x)"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = weightText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = accentLabelColor
                            )
                        }

                        Slider(
                            value = factor.weight.value,
                            onValueChange = { factor.weight.value = it },
                            valueRange = 1f..4f,
                            steps = 2,
                            colors = SliderDefaults.colors(
                                thumbColor = accentLabelColor,
                                activeTrackColor = accentLabelColor,
                                inactiveTrackColor = accentLabelColor.copy(alpha = 0.24f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("factor_slider_${factor.id}")
                        )
                    }
                }
            }
        }
    }
}
