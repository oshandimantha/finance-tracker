package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.CategorySpending
import com.example.domain.model.FinanceFormatter
import com.example.domain.model.MonthlyTrendPoint
import com.example.ui.theme.CurrencyTextStyleMedium
import com.example.ui.theme.CurrencyTextStyleSmall
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun InteractiveDonutChart(
    items: List<CategorySpending>,
    totalSpent: Double,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(items) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val fallbackColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val sliceAngles = remember(items, totalSpent) {
        var currentStart = -90f
        items.map { item ->
            val sweep = if (totalSpent > 0.0) ((item.amount / totalSpent) * 360.0).toFloat() else 0f
            val pair = currentStart to sweep
            currentStart += sweep
            pair
        }
    }

    val selectedItem = selectedIndex?.let { items.getOrNull(it) }

    Box(
        modifier = modifier
            .size(220.dp)
            .testTag("category_donut_chart"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items, sliceAngles) {
                    detectTapGestures { offset ->
                        if (items.isEmpty() || totalSpent <= 0.0) return@detectTapGestures
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val dist = hypot(dx, dy)
                        val outerR = minOf(size.width, size.height) / 2f
                        val innerR = outerR * 0.52f
                        if (dist in innerR..outerR) {
                            var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (deg < -90f) deg += 360f
                            val tapped = sliceAngles.indexOfFirst { (start, sweep) ->
                                deg >= start && deg <= start + sweep
                            }
                            if (tapped >= 0) {
                                onSelectIndex(if (selectedIndex == tapped) null else tapped)
                            }
                        } else {
                            onSelectIndex(null)
                        }
                    }
                }
        ) {
            val strokeWidth = size.minDimension * 0.19f
            val selectedExtra = size.minDimension * 0.03f
            val radius = (size.minDimension - strokeWidth - selectedExtra * 2) / 2f
            val topLeft = Offset(
                (size.width - radius * 2) / 2f,
                (size.height - radius * 2) / 2f
            )
            val arcSize = Size(radius * 2, radius * 2)

            if (items.isEmpty() || totalSpent <= 0.0) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                return@Canvas
            }

            items.forEachIndexed { index, item ->
                val (start, fullSweep) = sliceAngles[index]
                val animatedSweep = fullSweep * animationProgress.value
                val gap = if (items.size > 1 && animatedSweep > 4f) 2.5f else 0f
                val isSelected = selectedIndex == index
                val color = CategoryVisuals.parseColor(item.category.hexColor, fallbackColor)
                drawArc(
                    color = if (selectedIndex != null && !isSelected) color.copy(alpha = 0.45f) else color,
                    startAngle = start + gap / 2f,
                    sweepAngle = (animatedSweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = if (isSelected) strokeWidth + selectedExtra else strokeWidth,
                        cap = StrokeCap.Butt
                    )
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 36.dp)
        ) {
            Text(
                text = selectedItem?.category?.name ?: "Total Spent",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = FinanceFormatter.formatCompactLkr(selectedItem?.amount ?: totalSpent),
                style = CurrencyTextStyleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            if (selectedItem != null) {
                Text(
                    text = FinanceFormatter.formatPercent(selectedItem.percentage),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MonthlyExpenseBarChart(
    points: List<MonthlyTrendPoint>,
    modifier: Modifier = Modifier
) {
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(points) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    val maxVal = remember(points) { (points.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1000.0) }
    val barPrimary = MaterialTheme.colorScheme.primary
    val barHighlight = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        val activePoint = points.getOrNull(selectedIdx)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activePoint?.let { "${it.month} Expenses" } ?: "Tap any bar to inspect month",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (activePoint != null) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = FinanceFormatter.formatLkr(activePoint.amount),
                        style = CurrencyTextStyleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .testTag("monthly_expense_bar_chart")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isEmpty()) return@detectTapGestures
                            val slotWidth = size.width / points.size.toFloat()
                            val tapped = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                            selectedIdx = if (selectedIdx == tapped) -1 else tapped
                        }
                    }
            ) {
                val chartHeight = size.height - 16.dp.toPx()
                val slotWidth = size.width / points.size.coerceAtLeast(1).toFloat()
                val barWidth = (slotWidth * 0.54f).coerceAtLeast(8f)

                // Horizontal dashed gridlines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (step in 0..3) {
                    val y = chartHeight * (step / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (step < 3) dashEffect else null
                    )
                }

                points.forEachIndexed { idx, pt ->
                    val ratio = ((pt.amount / maxVal).toFloat() * anim.value).coerceIn(0f, 1f)
                    val barHeight = (chartHeight * ratio).coerceAtLeast(if (pt.amount > 0) 6f else 2f)
                    val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val top = chartHeight - barHeight
                    val isSelected = selectedIdx == idx
                    val barColor = if (isSelected) barHighlight else barPrimary

                    drawRoundRect(
                        color = if (selectedIdx >= 0 && !isSelected) barColor.copy(alpha = 0.4f) else barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, pt ->
                Text(
                    text = pt.month.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selectedIdx == idx) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedIdx == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun CategoryTrendLineChart(
    points: List<MonthlyTrendPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    var selectedIdx by remember(points) { mutableIntStateOf(-1) }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(points) {
        anim.snapTo(0f)
        anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    val maxVal = remember(points) { (points.maxOfOrNull { it.amount } ?: 0.0).coerceAtLeast(1000.0) }
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val pointCenterColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier.fillMaxWidth()) {
        val activePoint = points.getOrNull(selectedIdx)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activePoint?.let { "${it.month} Trend Value" } ?: "Tap chart to inspect monthly point",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (activePoint != null) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = lineColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = FinanceFormatter.formatLkr(activePoint.amount),
                        style = CurrencyTextStyleSmall,
                        color = lineColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .testTag("category_trend_line_chart")
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isEmpty()) return@detectTapGestures
                            val stepX = size.width / (points.size - 1).coerceAtLeast(1).toFloat()
                            val nearest = (offset.x / stepX).roundToInt().coerceIn(0, points.lastIndex)
                            selectedIdx = if (selectedIdx == nearest) -1 else nearest
                        }
                    }
            ) {
                if (points.isEmpty()) return@Canvas
                val chartHeight = size.height - 16.dp.toPx()
                val horizontalPadding = 10.dp.toPx()
                val usableWidth = (size.width - horizontalPadding * 2).coerceAtLeast(1f)
                val stepX = usableWidth / (points.size - 1).coerceAtLeast(1).toFloat()

                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                for (step in 0..3) {
                    val y = chartHeight * (step / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (step < 3) dashEffect else null
                    )
                }

                val coords = points.mapIndexed { idx, pt ->
                    val x = horizontalPadding + idx * stepX
                    val yRatio = ((pt.amount / maxVal).toFloat() * anim.value).coerceIn(0f, 1f)
                    val y = chartHeight - (chartHeight * yRatio)
                    Offset(x, y)
                }

                val linePath = Path().apply {
                    coords.forEachIndexed { idx, offset ->
                        if (idx == 0) moveTo(offset.x, offset.y)
                        else {
                            val prev = coords[idx - 1]
                            val midX = (prev.x + offset.x) / 2f
                            cubicTo(midX, prev.y, midX, offset.y, offset.x, offset.y)
                        }
                    }
                }

                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, chartHeight)
                    lineTo(coords.first().x, chartHeight)
                    close()
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                        startY = 0f,
                        endY = chartHeight
                    )
                )

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                coords.forEachIndexed { idx, offset ->
                    val isSelected = idx == selectedIdx
                    if (isSelected) {
                        drawLine(
                            color = lineColor.copy(alpha = 0.45f),
                            start = Offset(offset.x, 0f),
                            end = Offset(offset.x, chartHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = dashEffect
                        )
                    }
                    drawCircle(
                        color = lineColor,
                        radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                        center = offset
                    )
                    drawCircle(
                        color = pointCenterColor,
                        radius = if (isSelected) 3.dp.toPx() else 1.8.dp.toPx(),
                        center = offset
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, pt ->
                Text(
                    text = pt.month.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selectedIdx == idx) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedIdx == idx) lineColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
