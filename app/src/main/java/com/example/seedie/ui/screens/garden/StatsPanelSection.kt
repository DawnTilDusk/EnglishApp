package com.example.seedie.ui.screens.garden

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.seedie.ui.theme.AccentOrange
import com.example.seedie.ui.theme.PrimaryGreen
import com.example.seedie.ui.theme.SecondaryBrown
import com.example.seedie.ui.theme.gardenShadow
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class DonutSliceData(
    val label: String,
    val minutes: Int,
    val color: Color,
    val supporting: String
)

private data class TrendPointData(
    val label: String,
    val shortLabel: String,
    val value: Int
)

@Composable
fun StatsPanelSection(modifier: Modifier = Modifier) {
    val donutData = remember {
        listOf(
            DonutSliceData("记新词", 18, PrimaryGreen, "吸收新内容"),
            DonutSliceData("复习巩固", 15, AccentOrange, "稳定记忆曲线"),
            DonutSliceData("错题回看", 12, SecondaryBrown, "查漏补缺")
        )
    }
    val trendData = remember {
        listOf(
            TrendPointData("6/23", "6/23", 126),
            TrendPointData("6/24", "6/24", 138),
            TrendPointData("6/25", "6/25", 152),
            TrendPointData("6/26", "6/26", 149),
            TrendPointData("6/27", "6/27", 166),
            TrendPointData("6/28", "6/28", 174),
            TrendPointData("今天", "今天", 188)
        )
    }

    var selectedDonutIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTrendIndex by remember { mutableStateOf(trendData.lastIndex) }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DonutFocusCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            slices = donutData,
            selectedIndex = selectedDonutIndex,
            onSelectionChange = { tappedIndex ->
                selectedDonutIndex = if (selectedDonutIndex == tappedIndex) null else tappedIndex
            }
        )

        StatsCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            eyebrow = "近 7 天",
            title = "词汇量趋势",
            description = "支持点位选中查看阶段增长表现",
            accentColor = AccentOrange
        ) {
            LineChartSection(
                modifier = Modifier.fillMaxSize(),
                points = trendData,
                selectedIndex = selectedTrendIndex,
                onSelectionChange = { selectedTrendIndex = it }
            )
        }
    }
}

@Composable
private fun DonutFocusCard(
    modifier: Modifier = Modifier,
    slices: List<DonutSliceData>,
    selectedIndex: Int?,
    onSelectionChange: (Int) -> Unit
) {
    val cardShape = RoundedCornerShape(28.dp)
    val totalMinutes = remember(slices) { slices.sumOf { it.minutes } }
    val selectedSlice = selectedIndex?.let(slices::get)
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.gardenShadow(shape = cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                            PrimaryGreen.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = PrimaryGreen.copy(alpha = 0.10f),
                    shape = cardShape
                )
                .padding(24.dp)
        ) {
            Text(
                text = "学习时间分布",
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { detailsExpanded = !detailsExpanded }
                    .padding(vertical = 4.dp),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                DonutChart(
                    modifier = Modifier.fillMaxSize(),
                    slices = slices,
                    selectedIndex = selectedIndex,
                    totalMinutes = totalMinutes,
                    onSliceSelected = onSelectionChange,
                    showSupportingText = false
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = detailsExpanded,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(animationSpec = tween(220)) +
                        slideInVertically(
                            animationSpec = tween(260),
                            initialOffsetY = { -it / 2 }
                        ),
                    exit = fadeOut(animationSpec = tween(180)) +
                        slideOutVertically(
                            animationSpec = tween(220),
                            targetOffsetY = { -it / 3 }
                        )
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        tonalElevation = 3.dp,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "学习时间分布",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "收起",
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .clickable { detailsExpanded = false }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = PrimaryGreen
                                )
                            }

                            Text(
                                text = selectedSlice?.let {
                                    "${it.label} ${it.minutes} 分钟，当前占比 ${((it.minutes / totalMinutes.toFloat()) * 100).roundToInt()}%。"
                                } ?: "总计 $totalMinutes 分钟，当前详细分类与切换入口都收纳在这里。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                slices.forEachIndexed { index, slice ->
                                    val selected = index == selectedIndex
                                    val percent = ((slice.minutes / totalMinutes.toFloat()) * 100).roundToInt()
                                    LegendRow(
                                        label = slice.label,
                                        supporting = "${slice.minutes} 分钟 · $percent%",
                                        color = slice.color,
                                        selected = selected
                                    ) { onSelectionChange(index) }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            ) {
                                val footerText = selectedSlice?.let {
                                    "${it.supporting}，可继续点击其它图例切换饼图高亮。"
                                } ?: "这里集中展示说明和图例，默认页面只保留饼图主体。"
                                Text(
                                    text = footerText,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun StatsCard(
    modifier: Modifier = Modifier,
    eyebrow: String,
    title: String,
    description: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardShape = RoundedCornerShape(28.dp)
    Surface(
        modifier = modifier.gardenShadow(shape = cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                            accentColor.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.10f),
                    shape = cardShape
                )
                .padding(24.dp)
        ) {
            SectionPill(label = eyebrow, accentColor = accentColor)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun SectionPill(label: String, accentColor: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = accentColor
        )
    }
}

@Composable
private fun DonutChartSection(
    modifier: Modifier = Modifier,
    slices: List<DonutSliceData>,
    selectedIndex: Int?,
    onSelectionChange: (Int) -> Unit
) {
    val totalMinutes = remember(slices) { slices.sumOf { it.minutes } }
    val selectedSlice = selectedIndex?.let(slices::get)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DonutChart(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            slices = slices,
            selectedIndex = selectedIndex,
            totalMinutes = totalMinutes,
            onSliceSelected = onSelectionChange
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            slices.forEachIndexed { index, slice ->
                val selected = index == selectedIndex
                val percent = ((slice.minutes / totalMinutes.toFloat()) * 100).roundToInt()
                LegendRow(
                    label = slice.label,
                    supporting = "${slice.minutes} 分钟 · $percent%",
                    color = slice.color,
                    selected = selected
                ) { onSelectionChange(index) }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ) {
            val footerText = selectedSlice?.let {
                "${it.label} 当前占比最高关注 ${it.supporting}"
            } ?: "总计 $totalMinutes 分钟，点击图例可切换中心说明。"
            Text(
                text = footerText,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DonutChart(
    modifier: Modifier = Modifier,
    slices: List<DonutSliceData>,
    selectedIndex: Int?,
    totalMinutes: Int,
    onSliceSelected: (Int) -> Unit,
    showSupportingText: Boolean = true
) {
    var animationPlayed by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val colorScheme = MaterialTheme.colorScheme
    val trackColor = colorScheme.surfaceVariant.copy(alpha = 0.32f)
    val centerSurfaceColor = colorScheme.surface.copy(alpha = 0.98f)
    val onSurface = colorScheme.onSurface
    val onSurfaceVariant = colorScheme.onSurfaceVariant
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 1100),
        label = "DonutChartProgress"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
    }

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

            var startAngle = -90f
            slices.forEachIndexed { index, slice ->
                val fullSweep = slice.minutes / totalMinutes.toFloat() * 360f
                val sweepAngle = fullSweep * progress
                val isSelected = index == selectedIndex
                val alpha = if (selectedIndex == null || isSelected) 1f else 0.25f
                val strokeWidth = if (isSelected) baseStroke * 1.12f else baseStroke

                drawArc(
                    color = slice.color.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += fullSweep
            }
        }

        Surface(
            shape = CircleShape,
            color = centerSurfaceColor,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier
                    .size(118.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = selectedIndex, label = "DonutCenterText") { currentIndex ->
                    val headline = currentIndex?.let { "${slices[it].minutes}m" } ?: "${totalMinutes}m"
                    val title = currentIndex?.let { slices[it].label } ?: "今日总时长"
                    val supporting = if (showSupportingText) {
                        currentIndex?.let { slices[it].supporting } ?: "轻触环形图查看细分"
                    } else {
                        null
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = headline,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = onSurface
                        )
                        if (supporting != null) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = supporting,
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
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
    val containerColor = if (selected) color.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = color, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
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
private fun LineChartSection(
    modifier: Modifier = Modifier,
    points: List<TrendPointData>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit
) {
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val tooltipHorizontalOffset = with(density) { 40.dp.toPx() }
    val tooltipVerticalOffset = with(density) { 42.dp.toPx() }
    val horizontalPaddingPx = with(density) { 18.dp.toPx() }
    val topPaddingPx = with(density) { 20.dp.toPx() }
    val bottomPaddingPx = with(density) { 16.dp.toPx() }
    val selectedPoint = points[selectedIndex]
    val selectedPointOffset = rememberSelectedTrendOffset(
        chartSize = chartSize,
        points = points,
        selectedIndex = selectedIndex,
        horizontalPadding = horizontalPaddingPx,
        topPadding = topPaddingPx,
        bottomPadding = bottomPaddingPx
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniLegendChip(
                    label = "累计掌握",
                    tint = PrimaryGreen,
                    filled = false
                )
                MiniLegendChip(
                    label = "增长区间",
                    tint = PrimaryGreen,
                    filled = true
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = AccentOrange.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "${selectedPoint.label} · ${selectedPoint.value} 词",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AccentOrange
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LineChart(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { chartSize = it },
                points = points,
                selectedIndex = selectedIndex,
                onSelectionChange = onSelectionChange,
                horizontalPadding = horizontalPaddingPx,
                topPadding = topPaddingPx,
                bottomPadding = bottomPaddingPx
            )

            selectedPointOffset?.let { offset ->
                Surface(
                    modifier = Modifier.offset {
                        IntOffset(
                            x = (offset.x - tooltipHorizontalOffset).roundToInt().coerceAtLeast(0),
                            y = (offset.y - tooltipVerticalOffset).roundToInt().coerceAtLeast(0)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "+${selectedPoint.value}",
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
                val selected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) PrimaryGreen.copy(alpha = 0.12f) else Color.Transparent
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
                        text = point.shortLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (selected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniLegendChip(label: String, tint: Color, filled: Boolean) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (filled) tint.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = tint.copy(alpha = if (filled) 0f else 0.16f),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (filled) {
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 8.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    tint.copy(alpha = 0.34f),
                                    tint.copy(alpha = 0.08f)
                                )
                            ),
                            shape = RoundedCornerShape(999.dp)
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(2.dp)
                        .background(tint, RoundedCornerShape(999.dp))
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LineChart(
    modifier: Modifier = Modifier,
    points: List<TrendPointData>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    horizontalPadding: Float,
    topPadding: Float,
    bottomPadding: Float
) {
    var animationPlayed by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val colorScheme = MaterialTheme.colorScheme
    val gridLineColor = colorScheme.outline.copy(alpha = 0.12f)
    val pointSurfaceColor = colorScheme.surface
    val progress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "LineChartProgress"
    )

    LaunchedEffect(Unit) {
        animationPlayed = true
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
                color = gridLineColor,
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val animatedPoints = chartPoints.map { point ->
            Offset(
                x = point.x,
                y = baselineY - ((baselineY - point.y) * progress)
            )
        }

        val linePath = Path().apply {
            animatedPoints.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }
        val areaPath = Path().apply {
            addPath(linePath)
            lineTo(animatedPoints.last().x, baselineY)
            lineTo(animatedPoints.first().x, baselineY)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    PrimaryGreen.copy(alpha = 0.28f),
                    PrimaryGreen.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                startY = topPadding,
                endY = baselineY
            )
        )
        drawPath(
            path = linePath,
            color = PrimaryGreen,
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val selectedPoint = animatedPoints[selectedIndex]
        drawLine(
            color = AccentOrange.copy(alpha = 0.28f),
            start = Offset(selectedPoint.x, topPadding),
            end = Offset(selectedPoint.x, baselineY),
            strokeWidth = 1.5.dp.toPx()
        )

        animatedPoints.forEachIndexed { index, point ->
            val isSelected = index == selectedIndex
            if (isSelected) {
                drawCircle(
                    color = AccentOrange.copy(alpha = 0.18f),
                    radius = 14.dp.toPx(),
                    center = point
                )
            }
            drawCircle(
                color = pointSurfaceColor,
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

    val angle = Math.toDegrees(
        atan2(
            (tapOffset.y - center.y).toDouble(),
            (tapOffset.x - center.x).toDouble()
        )
    ).toFloat()
    val normalizedAngle = (angle + 450f) % 360f
    val total = slices.sumOf { it.minutes }.toFloat()
    var currentSweep = 0f
    slices.forEachIndexed { index, slice ->
        val sliceSweep = slice.minutes / total * 360f
        if (normalizedAngle in currentSweep..(currentSweep + sliceSweep)) {
            return index
        }
        currentSweep += sliceSweep
    }
    return null
}

private fun computeTrendOffsets(
    chartSize: Size,
    points: List<TrendPointData>,
    horizontalPadding: Float,
    topPadding: Float,
    bottomPadding: Float
): List<Offset> {
    if (points.isEmpty()) return emptyList()

    val maxValue = max(points.maxOf { it.value }, 1)
    val usableHeight = max(chartSize.height - topPadding - bottomPadding, 1f)
    val usableWidth = max(chartSize.width - horizontalPadding * 2f, 1f)
    val stepX = if (points.size == 1) 0f else usableWidth / (points.size - 1)

    return points.mapIndexed { index, point ->
        val ratio = point.value / maxValue.toFloat()
        Offset(
            x = horizontalPadding + stepX * index,
            y = chartSize.height - bottomPadding - ratio * usableHeight
        )
    }
}

private fun findNearestTrendPointIndex(
    touchOffset: Offset,
    canvasSize: IntSize,
    points: List<TrendPointData>,
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
        .mapIndexed { index, offset ->
            index to hypot(offset.x - touchOffset.x, offset.y - touchOffset.y)
        }
        .minByOrNull { it.second }
        ?.first
}

@Composable
private fun rememberSelectedTrendOffset(
    chartSize: IntSize,
    points: List<TrendPointData>,
    selectedIndex: Int,
    horizontalPadding: Float,
    topPadding: Float,
    bottomPadding: Float
): Offset? {
    if (chartSize == IntSize.Zero || selectedIndex !in points.indices) return null

    val offsets = computeTrendOffsets(
        chartSize = Size(chartSize.width.toFloat(), chartSize.height.toFloat()),
        points = points,
        horizontalPadding = horizontalPadding,
        topPadding = topPadding,
        bottomPadding = bottomPadding
    )
    return offsets.getOrNull(selectedIndex)
}
