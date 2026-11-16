package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ai.WeeklyRecapUiState
import com.example.domain.EpochDayUtils
import com.example.domain.ForecastEngine
import com.example.ui.components.EmptyTransactionsCard
import com.example.ui.components.heroCardGradientBrush
import com.example.ui.components.softCardGradientBrush
import com.example.ui.components.statusColors
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.BaonUiState
import com.example.ui.viewmodel.CategoryBreakdownItem
import com.example.ui.viewmodel.DailySpendPoint
import kotlin.math.max

@Composable
fun InsightsScreen(
    uiState: BaonUiState,
    recapState: WeeklyRecapUiState,
    onRefreshRecap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialYm = remember { EpochDayUtils.currentYearMonth() }
    var selectedYear by remember { mutableIntStateOf(initialYm.first) }
    var selectedMonth by remember { mutableIntStateOf(initialYm.second) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // AI Weekly Recap Card
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(heroCardGradientBrush())
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Weekly Recap",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            FilledTonalButton(
                                onClick = onRefreshRecap,
                                enabled = recapState !is WeeklyRecapUiState.Loading,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("refresh_ai_recap_button")
                            ) {
                                if (recapState is WeeklyRecapUiState.Loading) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refresh")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        when (recapState) {
                            is WeeklyRecapUiState.Idle -> {
                                Text(
                                    text = "Tap Refresh to get a short Taglish coaching note based on your recent gastos.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is WeeklyRecapUiState.Loading -> {
                                Text(
                                    text = "Analyzing your baon patterns...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is WeeklyRecapUiState.Ready -> {
                                Text(
                                    text = recapState.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            is WeeklyRecapUiState.Error -> {
                                Text(
                                    text = recapState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Category Breakdown Donut + Envelope Progress
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(softCardGradientBrush())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Cycle Category Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (uiState.categoryBreakdown.isEmpty() || uiState.totalCycleExpensesCentavos == 0L) {
                            EmptyTransactionsCard(
                                title = "No cycle expenses yet",
                                subtitle = "Log a few gastos to unlock your student category donut chart."
                            )
                        } else {
                            CategoryDonutChart(
                                breakdown = uiState.categoryBreakdown,
                                totalCentavos = uiState.totalCycleExpensesCentavos
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            uiState.categoryBreakdown.forEach { item ->
                                CategoryBreakdownRow(item = item)
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }

            // 7-Day Spending Bar Chart
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(softCardGradientBrush())
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last 7 Days",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Target ${ForecastEngine.formatPeso(uiState.forecast.dailyTargetCentavos)}/day",
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        SevenDayBarChart(
                            points = uiState.last7DaysSpend,
                            dailyTargetCentavos = uiState.forecast.dailyTargetCentavos
                        )
                    }
                }
            }

            // Calendar Heatmap
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(softCardGradientBrush())
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spending Heatmap",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (selectedMonth == 0) {
                                            selectedMonth = 11
                                            selectedYear -= 1
                                        } else {
                                            selectedMonth -= 1
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous month"
                                    )
                                }
                                Text(
                                    text = EpochDayUtils.formatMonthYear(selectedYear, selectedMonth),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = {
                                        if (selectedMonth == 11) {
                                            selectedMonth = 0
                                            selectedYear += 1
                                        } else {
                                            selectedMonth += 1
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next month"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        CalendarHeatmapGrid(
                            year = selectedYear,
                            monthZeroBased = selectedMonth,
                            dailySpendByEpochDay = uiState.dailySpendByEpochDay,
                            dailyTargetCentavos = uiState.forecast.dailyTargetCentavos
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDonutChart(
    breakdown: List<CategoryBreakdownItem>,
    totalCentavos: Long
) {
    val activeItems = breakdown.filter { it.spentCentavos > 0L }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val strokePx = 26.dp.toPx()
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            var startAngle = -90f
            activeItems.forEach { item ->
                val sweep = (item.percentage * 360f).coerceAtLeast(3f)
                drawArc(
                    color = Color(item.category.colorHex),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "TOTAL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = ForecastEngine.formatPeso(totalCentavos),
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CategoryBreakdownRow(item: CategoryBreakdownItem) {
    val catColor = Color(item.category.colorHex)
    val pctText = "${(item.percentage * 100).toInt()}%"
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(catColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pctText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = ForecastEngine.formatPeso(item.spentCentavos),
                fontFamily = JetBrainsMonoFontFamily,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        val envelope = item.category.envelopeCentavos
        if (envelope != null && envelope > 0L) {
            Spacer(modifier = Modifier.height(4.dp))
            val ratio = (item.spentCentavos.toFloat() / envelope.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { ratio },
                color = if (item.spentCentavos > envelope) MaterialTheme.colorScheme.error else catColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
            )
            Text(
                text = "Envelope cap: ${ForecastEngine.formatPeso(envelope)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SevenDayBarChart(
    points: List<DailySpendPoint>,
    dailyTargetCentavos: Long
) {
    val maxSpend = max(
        points.maxOfOrNull { it.spentCentavos } ?: 0L,
        max(dailyTargetCentavos, 100_00L)
    ).coerceAtLeast(1L)

    val primaryColor = MaterialTheme.colorScheme.primary
    val overColor = MaterialTheme.colorScheme.error
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val targetLineColor = MaterialTheme.colorScheme.secondary

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val barCount = points.size.coerceAtLeast(1)
            val slotWidth = size.width / barCount
            val barWidth = (slotWidth * 0.48f).coerceAtMost(32.dp.toPx())

            points.forEachIndexed { idx, pt ->
                val ratio = (pt.spentCentavos.toFloat() / maxSpend.toFloat()).coerceIn(0f, 1f)
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f

                // Background rail
                drawRoundRect(
                    color = trackColor.copy(alpha = 0.55f),
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )

                if (pt.spentCentavos > 0L) {
                    val barHeight = (size.height * ratio).coerceAtLeast(6.dp.toPx())
                    val isOver = dailyTargetCentavos > 0L && pt.spentCentavos > dailyTargetCentavos
                    drawRoundRect(
                        color = if (isOver) overColor else primaryColor,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
            }

            if (dailyTargetCentavos > 0L) {
                val targetRatio = (dailyTargetCentavos.toFloat() / maxSpend.toFloat()).coerceIn(0f, 1f)
                val y = size.height - (size.height * targetRatio)
                drawLine(
                    color = targetLineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CalendarHeatmapGrid(
    year: Int,
    monthZeroBased: Int,
    dailySpendByEpochDay: Map<Long, Long>,
    dailyTargetCentavos: Long
) {
    val cells = remember(year, monthZeroBased) {
        EpochDayUtils.calendarDaysForMonth(year, monthZeroBased)
    }
    val today = remember { EpochDayUtils.todayEpochDay() }
    val weeks = cells.chunked(7)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (i in 0 until 7) {
                    val cell = week.getOrNull(i)
                    if (cell == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    } else {
                        val (dayNum, epochDay) = cell
                        val spend = dailySpendByEpochDay[epochDay] ?: 0L
                        val hasSpend = spend > 0L
                        val (bg, fg) = if (hasSpend) {
                            statusColors(ForecastEngine.statusForDay(spend, dailyTargetCentavos))
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = bg,
                            border = if (epochDay == today) {
                                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dayNum.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (hasSpend || epochDay == today) FontWeight.Bold else FontWeight.Normal,
                                    color = fg
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
