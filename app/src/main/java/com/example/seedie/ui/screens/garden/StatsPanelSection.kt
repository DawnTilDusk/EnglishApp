package com.example.seedie.ui.screens.garden

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.seedie.ui.components.TabSectionSurface
import com.example.seedie.ui.theme.AccentOrange
import com.example.seedie.ui.theme.PrimaryGreen
import com.example.seedie.ui.theme.SecondaryBrown
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class DonutSliceData(
    val label: String,
    val durationSec: Int,
    val percentage: Int,
    val color: Color
)

@Composable
fun StatsPanelSection(
    modifier: Modifier = Modifier,
    trendReplayKey: Int = 0,
    activityStats: GardenActivityStatsUiState
) {
    val totalDurationSec = activityStats.totalActiveDurationSec
    val slices = remember(activityStats.moduleDurations, totalDurationSec) {
        activityStats.moduleDurations.mapIndexed { index, item ->
            DonutSliceData(
                label = item.label,
                durationSec = item.durationSec,
                percentage = if (totalDurationSec > 0) {
                    ((item.durationSec * 100f) / totalDurationSec).roundToInt().coerceAtLeast(1)
                } else {
                    0
                },
                color = donutColorFor(index)
            )
        }
    }
    val safeTrendPoints = remember(activityStats.trendPoints) {
        if (activityStats.trendPoints.isEmpty()) {
            List(7) { index ->
                GardenTrendPointUiState(
                    date = index.toString(),
                    label = "",
                    shortLabel = "",
                    durationSec = 0
                )
            }
        } else {
            activityStats.trendPoints
        }
    }

    var selectedDonutIndex by remember(slices) {
        mutableStateOf<Int?>(slices.indices.firstOrNull())
    }
    var selectedTrendIndex by remember(safeTrendPoints) {
        mutableIntStateOf(safeTrendPoints.lastIndex.coerceAtLeast(0))
    }
    var trendRefreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(trendReplayKey) {
        if (trendReplayKey > 0) {
            trendRefreshKey += 1
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TodayActivityCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            summary = activityStats,
            slices = slices,
            selectedIndex = selectedDonutIndex,
            onSelectionChange = { selectedDonutIndex = it }
        )

        ActivityTrendCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            points = safeTrendPoints,
            selectedIndex = selectedTrendIndex,
            refreshKey = trendRefreshKey,
            onSelectionChange = { selectedTrendIndex = it }
        )
    }
}

@Composable
private fun TodayActivityCard(
    modifier: Modifier,
    summary: GardenActivityStatsUiState,
    slices: List<DonutSliceData>,
    selectedIndex: Int?,
    onSelectionChange: (Int?) -> Unit
) {
    val selectedSlice = selectedIndex?.let(slices::getOrNull)
    val topModule = summary.moduleDurations.firstOrNull()

    TabSectionSurface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        accentColor = PrimaryGreen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "今日全局活跃",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (summary.hasActivity) {
                    topModule?.let {
                        "累计 ${formatDuration(summary.totalActiveDurationSec)}，停留最久的是${it.label}。"
                    } ?: "累计 ${formatDuration(summary.totalActiveDurationSec)}。"
                } else {
                    summary.emptyStateText
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(
                    modifier = Modifier.fillMaxSize(),
                    slices = slices,
                    selectedIndex = selectedIndex,
                    totalDurationSec = summary.totalActiveDurationSec,
                    onSliceSelected = { index ->
                        onSelectionChange(if (selectedIndex == index) null else index)
                    }
                )
            }

            if (summary.hasActivity) {
                val detailText = selectedSlice?.let {
                    "${it.label} ${formatDuration(it.durationSec)}，占比 ${it.percentage}%"
                } ?: topModule?.let {
                    "分布覆盖 ${summary.moduleDurations.size} 个模块，最高占比 ${slices.firstOrNull()?.percentage ?: 0}%"
                }.orEmpty()
                Text(
                    text = detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slices.take(4).forEachIndexed { index, slice ->
                        LegendRow(
                            label = slice.label,
                            supporting = "${formatDuration(slice.durationSec)} · ${slice.percentage}%",
                            color = slice.color,
                            selected = index == selectedIndex
                        ) {
                            onSelectionChange(if (selectedIndex == index) null else index)
                        }
                    }
                }
            } else {
                EmptyHintCard(
                    text = "空状态下不再展示演示值，等真实活跃数据写入后这里会自动刷新。"
                )
            }

            Text(
                text = summary.effectiveStudyHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityTrendCard(
    modifier: Modifier,
    points: List<GardenTrendPointUiState>,
    selectedIndex: Int,
    refreshKey: Int,
    onSelectionChange: (Int) -> Unit
) {
    val safeSelectedIndex = if (points.isEmpty()) 0 else selectedIndex.coerceIn(points.indices)
    val selectedPoint = points.getOrNull(safeSelectedIndex)
    val hasTrendData = points.any { it.durationSec > 0 }

    TabSectionSurface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        accentColor = AccentOrange
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "近 7 天活跃趋势",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (hasTrendData && selectedPoint != null) {
                    "${selectedPoint.label.ifBlank { selectedPoint.shortLabel }}活跃 ${formatDuration(selectedPoint.durationSec)}"
                } else {
                    "近 7 天暂无活跃记录，趋势图已按零值稳定补齐。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ActivityTrendChartSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                points = points,
                selectedIndex = safeSelectedIndex,
                refreshKey = refreshKey,
                onSelectionChange = onSelectionChange
            )
        }
    }
}

@Composable
private fun DonutChart(
    modifier: Modifier,
    slices: List<DonutSliceData>,
    selectedIndex: Int?,
    totalDurationSec: Int,
    onSliceSelected: (Int) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.82f)
                .onSizeChanged { canvasSize = it }
                .pointerInput(slices, selectedIndex) {
                    detectTapGestures { offset ->
                        detectDonutSliceIndex(
                            tapOffset = offset,
                            canvasSize = canvasSize,
                            slices = slices
                        )?.let(onSliceSelected)
                    }
                }
        ) {
            val minDimension = min(size.width, size.height)
            val baseStroke = minDimension * 0.16f
            val arcSize = Size(minDimension - baseStroke, minDimension - baseStroke)
            val topLeft = Offset(
                x = (size.width - arcSize.width) / 2f,
                y = (size.height - arcSize.height) / 2f
            )

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = baseStroke, cap = StrokeCap.Round)
            )

            if (slices.isNotEmpty() && totalDurationSec > 0) {
                var startAngle = -90f
                slices.forEachIndexed { index, slice ->
                    val fullSweep = slice.durationSec / totalDurationSec.toFloat() * 360f
                    val isSelected = index == selectedIndex
                    val alpha = if (selectedIndex == null || isSelected) 1f else 0.28f
                    val strokeWidth = if (isSelected) baseStroke * 1.12f else baseStroke
                    drawArc(
                        color = slice.color.copy(alpha = alpha),
                        startAngle = startAngle,
                        sweepAngle = fullSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += fullSweep
                }
            }
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ) {
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedIndex != null) {
                            formatDurationCompact(slices[selectedIndex].durationSec)
                        } else {
                            formatDurationCompact(totalDurationSec)
                        },
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedIndex != null) slices[selectedIndex].label else "今日总时长",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTrendChartSection(
    modifier: Modifier,
    points: List<GardenTrendPointUiState>,
    selectedIndex: Int,
    refreshKey: Int,
    onSelectionChange: (Int) -> Unit
) {
    val density = LocalDensity.current
    val safeSelectedIndex = if (points.isEmpty()) 0 else selectedIndex.coerceIn(points.indices)
    val selectedPoint = points.getOrNull(safeSelectedIndex)
    val horizontalPaddingPx = with(density) { 18.dp.toPx() }
    val topPaddingPx = with(density) { 8.dp.toPx() }
    val bottomPaddingPx = with(density) { 12.dp.toPx() }
    val tooltipVisible by produceState(initialValue = false, key1 = refreshKey, key2 = points) {
        delay(520)
        value = true
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            ActivityTrendChart(
                modifier = Modifier.fillMaxSize(),
                points = points,
                selectedIndex = safeSelectedIndex,
                refreshKey = refreshKey,
                horizontalPadding = horizontalPaddingPx,
                topPadding = topPaddingPx,
                bottomPadding = bottomPaddingPx,
                onSelectionChange = onSelectionChange
            )

            if (tooltipVisible && selectedPoint != null) {
                Surface(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "${selectedPoint.shortLabel.ifBlank { "--" }} ${formatDuration(selectedPoint.durationSec)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryGreen
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            points.forEachIndexed { index, point ->
                val selected = index == safeSelectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selected) PrimaryGreen.copy(alpha = 0.12f) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelectionChange(index) }
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = point.shortLabel.ifBlank { "--" },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (selected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTrendChart(
    modifier: Modifier,
    points: List<GardenTrendPointUiState>,
    selectedIndex: Int,
    refreshKey: Int,
    horizontalPadding: Float,
    topPadding: Float,
    bottomPadding: Float,
    onSelectionChange: (Int) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val revealProgress = remember { Animatable(0f) }
    val safeSelectedIndex = if (points.isEmpty()) 0 else selectedIndex.coerceIn(points.indices)

    LaunchedEffect(refreshKey, points) {
        revealProgress.snapTo(0f)
        revealProgress.animateTo(1f, animationSpec = tween(durationMillis = 760))
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(points, canvasSize) {
                detectTapGestures { offset ->
                    findNearestTrendPointIndex(
                        touchOffset = offset,
                        canvasSize = canvasSize,
                        points = points,
                        horizontalPadding = horizontalPadding,
                        topPadding = topPadding,
                        bottomPadding = bottomPadding
                    )?.let(onSelectionChange)
                }
            }
            .pointerInput(points, canvasSize) {
                detectDragGestures(
                    onDragStart = { offset ->
                        findNearestTrendPointIndex(
                            touchOffset = offset,
                            canvasSize = canvasSize,
                            points = points,
                            horizontalPadding = horizontalPadding,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding
                        )?.let(onSelectionChange)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        findNearestTrendPointIndex(
                            touchOffset = change.position,
                            canvasSize = canvasSize,
                            points = points,
                            horizontalPadding = horizontalPadding,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding
                        )?.let(onSelectionChange)
                    }
                )
            }
    ) {
        val chartPoints = computeTrendOffsets(
            chartSize = size,
            points = points,
            horizontalPadding = horizontalPadding,
            topPadding = topPadding,
            bottomPadding = bottomPadding
        )
        if (chartPoints.isEmpty()) return@Canvas

        val baselineY = size.height - bottomPadding
        repeat(4) { index ->
            val ratio = index / 3f
            val y = topPadding + (baselineY - topPadding) * ratio
            drawLine(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val visiblePoints = buildVisibleTrendPoints(chartPoints, revealProgress.value)
        if (visiblePoints.size > 1) {
            drawPath(
                path = buildTrendAreaPath(visiblePoints, baselineY),
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PrimaryGreen.copy(alpha = 0.24f),
                        PrimaryGreen.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    startY = topPadding,
                    endY = baselineY
                )
            )
            drawPath(
                path = buildTrendLinePath(visiblePoints),
                color = PrimaryGreen,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        val revealX = visiblePoints.last().x
        val selectedPoint = chartPoints[safeSelectedIndex]
        if (selectedPoint.x <= revealX) {
            drawLine(
                color = AccentOrange.copy(alpha = 0.28f),
                start = Offset(selectedPoint.x, topPadding),
                end = Offset(selectedPoint.x, baselineY),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        chartPoints.forEachIndexed { index, point ->
            if (point.x > revealX) return@forEachIndexed
            val isSelected = index == safeSelectedIndex
            if (isSelected) {
                drawCircle(
                    color = AccentOrange.copy(alpha = 0.18f),
                    radius = 14.dp.toPx(),
                    center = point
                )
            }
            drawCircle(
                color = MaterialTheme.colorScheme.surface,
                radius = if (isSelected) 8.dp.toPx() else 6.dp.toPx(),
                center = point
            )
            drawCircle(
                color = if (isSelected) AccentOrange else PrimaryGreen,
                radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
private fun LegendRow(
    label: String,
    supporting: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (selected) "已选" else "查看",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyHintCard(text: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun detectDonutSliceIndex(
    tapOffset: Offset,
    canvasSize: IntSize,
    slices: List<DonutSliceData>
): Int? {
    if (canvasSize == IntSize.Zero || slices.isEmpty()) return null
    val width = canvasSize.width.toFloat()
    val height = canvasSize.height.toFloat()
    val minDimension = min(width, height)
    val strokeWidth = minDimension * 0.16f
    val center = Offset(width / 2f, height / 2f)
    val distance = hypot(tapOffset.x - center.x, tapOffset.y - center.y)
    val outerRadius = minDimension / 2f
    val innerRadius = outerRadius - strokeWidth
    if (distance !in innerRadius..outerRadius) return null

    val total = slices.sumOf { it.durationSec }.toFloat()
    if (total <= 0f) return null
    val angle = (Math.toDegrees(
        kotlin.math.atan2(
            (tapOffset.y - center.y).toDouble(),
            (tapOffset.x - center.x).toDouble()
        )
    ) + 450.0) % 360.0
    var currentSweep = 0f
    slices.forEachIndexed { index, slice ->
        val sliceSweep = slice.durationSec / total * 360f
        if (angle.toFloat() in currentSweep..(currentSweep + sliceSweep)) return index
        currentSweep += sliceSweep
    }
    return null
}

private fun computeTrendOffsets(
    chartSize: Size,
    points: List<GardenTrendPointUiState>,
    horizontalPadding: Float,
    topPadding: Float,
    bottomPadding: Float
): List<Offset> {
    if (points.isEmpty()) return emptyList()
    val maxValue = max(points.maxOf { it.durationSec }, 1)
    val usableHeight = max(chartSize.height - topPadding - bottomPadding, 1f)
    val usableWidth = max(chartSize.width - horizontalPadding * 2f, 1f)
    val stepX = if (points.size == 1) 0f else usableWidth / (points.size - 1)
    return points.mapIndexed { index, point ->
        val ratio = point.durationSec / maxValue.toFloat()
        Offset(
            x = horizontalPadding + stepX * index,
            y = chartSize.height - bottomPadding - ratio * usableHeight
        )
    }
}

private fun findNearestTrendPointIndex(
    touchOffset: Offset,
    canvasSize: IntSize,
    points: List<GardenTrendPointUiState>,
    horizontalPadding: Float,
    topPadding: Float,
    bottomPadding: Float
): Int? {
    if (canvasSize == IntSize.Zero || points.isEmpty()) return null
    val offsets = computeTrendOffsets(
        chartSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
        points = points,
        horizontalPadding = horizontalPadding,
        topPadding = topPadding,
        bottomPadding = bottomPadding
    )
    return offsets
        .mapIndexed { index, offset -> index to hypot(offset.x - touchOffset.x, offset.y - touchOffset.y) }
        .minByOrNull { it.second }
        ?.first
}

private fun buildVisibleTrendPoints(points: List<Offset>, revealProgress: Float): List<Offset> {
    if (points.isEmpty()) return emptyList()
    if (points.size == 1 || revealProgress >= 1f) return points
    if (revealProgress <= 0f) return listOf(points.first())
    val segmentProgress = revealProgress * (points.size - 1)
    val lastFullIndex = segmentProgress.toInt().coerceIn(0, points.lastIndex)
    val remainder = segmentProgress - lastFullIndex
    val visible = points.take(lastFullIndex + 1).toMutableList()
    if (lastFullIndex < points.lastIndex) {
        val start = points[lastFullIndex]
        val end = points[lastFullIndex + 1]
        visible += Offset(
            x = start.x + (end.x - start.x) * remainder,
            y = start.y + (end.y - start.y) * remainder
        )
    }
    return visible
}

private fun buildTrendLinePath(points: List<Offset>): Path {
    return Path().apply {
        points.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
        }
    }
}

private fun buildTrendAreaPath(points: List<Offset>, baselineY: Float): Path {
    return Path().apply {
        addPath(buildTrendLinePath(points))
        lineTo(points.last().x, baselineY)
        lineTo(points.first().x, baselineY)
        close()
    }
}

private fun donutColorFor(index: Int): Color {
    return when (index % 4) {
        0 -> PrimaryGreen
        1 -> AccentOrange
        2 -> SecondaryBrown
        else -> PrimaryGreen.copy(alpha = 0.7f)
    }
}

private fun formatDuration(durationSec: Int): String {
    if (durationSec <= 0) return "0 分钟"
    val totalMinutes = (durationSec / 60).coerceAtLeast(0)
    if (totalMinutes <= 0) return "少于 1 分钟"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours} 小时 ${minutes} 分钟"
        hours > 0 -> "${hours} 小时"
        else -> "${minutes} 分钟"
    }
}

private fun formatDurationCompact(durationSec: Int): String {
    if (durationSec <= 0) return "0m"
    val totalMinutes = (durationSec / 60).coerceAtLeast(0)
    if (totalMinutes <= 0) return "<1m"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        "${hours}h${if (minutes > 0) "${minutes}m" else ""}"
    } else {
        "${minutes}m"
    }
}
