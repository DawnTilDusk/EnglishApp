package com.example.seedie.ui.screens.garden

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.seedie.ui.theme.AccentOrange
import com.example.seedie.ui.theme.PrimaryGreen
import com.example.seedie.ui.theme.SecondaryBrown
import com.example.seedie.ui.theme.gardenShadow
import kotlinx.coroutines.delay
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

private data class TrendSeriesData(
    val points: List<TrendPointData>
)

private enum class TrendRangeOption(val label: String) {
    Last7Days("近 7 天"),
    Last30Days("近 30 天"),
    LastYear("近 1 年"),
    ThisMonth("本月"),
    ThisYear("本年度")
}

private enum class TrendMetricOption(val label: String) {
    Cumulative("累计掌握"),
    Growth("增长区间")
}

private enum class TrendFilterMenuType {
    Range,
    Metric
}

@Composable
fun StatsPanelSection(
    modifier: Modifier = Modifier,
    trendReplayKey: Int = 0
) {
    val donutData = remember {
        listOf(
            DonutSliceData("记新词", 18, PrimaryGreen, "吸收新内容"),
            DonutSliceData("复习巩固", 15, AccentOrange, "稳定记忆曲线"),
            DonutSliceData("错题回看", 12, SecondaryBrown, "查漏补缺")
        )
    }
    val trendCatalog = remember { buildTrendSeriesCatalog() }

    var selectedDonutIndex by remember { mutableStateOf<Int?>(null) }
    var selectedRange by remember { mutableStateOf(TrendRangeOption.Last7Days) }
    var selectedMetric by remember { mutableStateOf(TrendMetricOption.Cumulative) }
    var expandedMenu by remember { mutableStateOf<TrendFilterMenuType?>(null) }
    var trendRefreshKey by remember { mutableIntStateOf(0) }
    val trendSeries = remember(selectedRange, selectedMetric) {
        trendCatalog.getValue(selectedRange to selectedMetric)
    }
    var selectedTrendIndex by remember(trendSeries.points) {
        mutableIntStateOf(trendSeries.points.lastIndex.coerceAtLeast(0))
    }

    LaunchedEffect(trendReplayKey) {
        if (trendReplayKey > 0) {
            trendRefreshKey += 1
        }
    }

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

        TrendFocusCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            selectedRange = selectedRange,
            selectedMetric = selectedMetric,
            expandedMenu = expandedMenu,
            points = trendSeries.points,
            selectedIndex = selectedTrendIndex,
            refreshKey = trendRefreshKey,
            onSelectionChange = { selectedTrendIndex = it },
            onMenuToggle = { menuType ->
                expandedMenu = if (expandedMenu == menuType) null else menuType
            },
            onMenuDismiss = { expandedMenu = null },
            onRangeSelect = { nextRange ->
                expandedMenu = null
                if (nextRange != selectedRange) {
                    selectedRange = nextRange
                    selectedTrendIndex = 0
                    trendRefreshKey += 1
                }
            },
            onMetricSelect = { nextMetric ->
                expandedMenu = null
                if (nextMetric != selectedMetric) {
                    selectedMetric = nextMetric
                    selectedTrendIndex = 0
                    trendRefreshKey += 1
                }
            }
        )
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
        Box(
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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

                DonutChart(
                    modifier = Modifier.fillMaxSize(),
                    slices = slices,
                    selectedIndex = selectedIndex,
                    totalMinutes = totalMinutes,
                    onSliceSelected = onSelectionChange
                )
            }

            AnimatedVisibility(
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
                        .clip(cardShape),
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    tonalElevation = 3.dp,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "学习时间分布",
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { detailsExpanded = false }
                                .padding(vertical = 4.dp),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

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

@Composable
private fun TrendFocusCard(
    modifier: Modifier = Modifier,
    selectedRange: TrendRangeOption,
    selectedMetric: TrendMetricOption,
    expandedMenu: TrendFilterMenuType?,
    points: List<TrendPointData>,
    selectedIndex: Int,
    refreshKey: Int,
    onSelectionChange: (Int) -> Unit,
    onMenuToggle: (TrendFilterMenuType) -> Unit,
    onMenuDismiss: () -> Unit,
    onRangeSelect: (TrendRangeOption) -> Unit,
    onMetricSelect: (TrendMetricOption) -> Unit
) {
    val cardShape = RoundedCornerShape(28.dp)
    val safeSelectedIndex = selectedIndex.coerceIn(points.indices)
    val density = LocalDensity.current
    var cardRootPosition by remember { mutableStateOf(Offset.Zero) }
    var rangeMenuAnchor by remember { mutableStateOf(IntOffset.Zero) }
    var metricMenuAnchor by remember { mutableStateOf(IntOffset.Zero) }
    var rangeMenuWidth by remember { mutableIntStateOf(0) }
    var metricMenuWidth by remember { mutableIntStateOf(0) }

    Surface(
        modifier = modifier.gardenShadow(shape = cardShape),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                            AccentOrange.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = AccentOrange.copy(alpha = 0.10f),
                    shape = cardShape
                )
                .padding(24.dp)
                .onGloballyPositioned { coordinates ->
                    cardRootPosition = coordinates.positionInRoot()
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "词汇量趋势",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        TrendFilterMenuButton(
                            label = selectedRange.label,
                            expanded = expandedMenu == TrendFilterMenuType.Range,
                            rootPosition = cardRootPosition,
                            onAnchorMeasured = { anchor, width ->
                                rangeMenuAnchor = anchor
                                rangeMenuWidth = width
                            },
                            onToggle = { onMenuToggle(TrendFilterMenuType.Range) }
                        )
                        TrendFilterMenuButton(
                            label = selectedMetric.label,
                            expanded = expandedMenu == TrendFilterMenuType.Metric,
                            rootPosition = cardRootPosition,
                            onAnchorMeasured = { anchor, width ->
                                metricMenuAnchor = anchor
                                metricMenuWidth = width
                            },
                            onToggle = { onMenuToggle(TrendFilterMenuType.Metric) }
                        )
                    }
                }

                LineChartSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    points = points,
                    selectedIndex = safeSelectedIndex,
                    selectedMetric = selectedMetric,
                    refreshKey = refreshKey,
                    onSelectionChange = onSelectionChange
                )
            }

            when (expandedMenu) {
                TrendFilterMenuType.Range -> {
                    TrendFilterOverlay(
                        modifier = Modifier
                            .offset { rangeMenuAnchor }
                            .width(with(density) { rangeMenuWidth.toDp() })
                            .zIndex(3f),
                        options = TrendRangeOption.entries,
                        selectedOption = selectedRange,
                        optionLabel = { it.label },
                        onSelect = {
                            onRangeSelect(it)
                            onMenuDismiss()
                        }
                    )
                }

                TrendFilterMenuType.Metric -> {
                    TrendFilterOverlay(
                        modifier = Modifier
                            .offset { metricMenuAnchor }
                            .width(with(density) { metricMenuWidth.toDp() })
                            .zIndex(3f),
                        options = TrendMetricOption.entries,
                        selectedOption = selectedMetric,
                        optionLabel = { it.label },
                        onSelect = {
                            onMetricSelect(it)
                            onMenuDismiss()
                        }
                    )
                }

                null -> Unit
            }
        }
    }
}

@Composable
private fun TrendFilterMenuButton(
    label: String,
    expanded: Boolean,
    rootPosition: Offset,
    onAnchorMeasured: (IntOffset, Int) -> Unit,
    onToggle: () -> Unit
) {
    val density = LocalDensity.current
    val buttonShape = RoundedCornerShape(16.dp)
    val buttonContainerColor = AccentOrange.copy(alpha = if (expanded) 0.18f else 0.10f)
    val dropdownGapPx = with(density) { 8.dp.roundToPx() }

    Box(
        modifier = Modifier.zIndex(if (expanded) 2f else 0f),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            modifier = Modifier
                .clip(buttonShape)
                .clickable(onClick = onToggle)
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    onAnchorMeasured(
                        IntOffset(
                            x = (position.x - rootPosition.x).roundToInt(),
                            y = (position.y - rootPosition.y).roundToInt() + coordinates.size.height + dropdownGapPx
                        ),
                        coordinates.size.width
                    )
                },
            shape = buttonShape,
            color = buttonContainerColor
        ) {
            Row(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = AccentOrange.copy(alpha = if (expanded) 0.26f else 0.16f),
                        shape = buttonShape
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (expanded) "^" else "v",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentOrange
                )
            }
        }
    }
}

@Composable
private fun <T> TrendFilterOverlay(
    modifier: Modifier = Modifier,
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    val selectedRowColor = AccentOrange.copy(alpha = 0.12f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            options.forEach { option ->
                val selected = option == selectedOption
                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) selectedRowColor else Color.Transparent)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onSelect(option) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = optionLabel(option),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (selected) AccentOrange else MaterialTheme.colorScheme.onSurface
                    )
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentOrange)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    modifier: Modifier = Modifier,
    slices: List<DonutSliceData>,
    selectedIndex: Int?,
    totalMinutes: Int,
    onSliceSelected: (Int) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val colorScheme = MaterialTheme.colorScheme
    val trackColor = colorScheme.surfaceVariant.copy(alpha = 0.32f)
    val centerSurfaceColor = colorScheme.surface.copy(alpha = 0.98f)
    val onSurface = colorScheme.onSurface
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1100),
        label = "DonutChartProgress"
    )

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
    selectedMetric: TrendMetricOption,
    refreshKey: Int,
    onSelectionChange: (Int) -> Unit
) {
    val chartSizeState = remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val safeSelectedIndex = selectedIndex.coerceIn(points.indices)
    val tooltipHorizontalOffset = with(density) { 44.dp.toPx() }
    val tooltipVerticalOffset = with(density) { 46.dp.toPx() }
    val horizontalPaddingPx = with(density) { 18.dp.toPx() }
    val topPaddingPx = with(density) { 8.dp.toPx() }
    val bottomPaddingPx = with(density) { 12.dp.toPx() }
    val selectedPoint = points[safeSelectedIndex]
    val selectedPointOffset = rememberSelectedTrendOffset(
        chartSize = chartSizeState.value,
        points = points,
        selectedIndex = safeSelectedIndex,
        horizontalPadding = horizontalPaddingPx,
        topPadding = topPaddingPx,
        bottomPadding = bottomPaddingPx
    )
    val tooltipVisible by produceState(initialValue = false, key1 = refreshKey) {
        delay(720)
        value = true
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LineChart(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { chartSizeState.value = it },
                points = points,
                selectedIndex = safeSelectedIndex,
                refreshKey = refreshKey,
                onSelectionChange = onSelectionChange,
                horizontalPadding = horizontalPaddingPx,
                topPadding = topPaddingPx,
                bottomPadding = bottomPaddingPx
            )

            if (tooltipVisible) {
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
                            text = formatTrendValue(selectedMetric, selectedPoint.value),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = PrimaryGreen
                        )
                    }
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
private fun LineChart(
    modifier: Modifier = Modifier,
    points: List<TrendPointData>,
    selectedIndex: Int,
    refreshKey: Int,
    onSelectionChange: (Int) -> Unit,
    horizontalPadding: Float,
    topPadding: Float,
    bottomPadding: Float
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val colorScheme = MaterialTheme.colorScheme
    val gridLineColor = colorScheme.outline.copy(alpha = 0.12f)
    val pointSurfaceColor = colorScheme.surface
    val revealProgress = remember { Animatable(0f) }
    val safeSelectedIndex = selectedIndex.coerceIn(points.indices)

    LaunchedEffect(refreshKey, points) {
        revealProgress.snapTo(0f)
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 860)
        )
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

        val revealValue = revealProgress.value
        val visiblePoints = buildVisibleTrendPoints(chartPoints, revealValue)
        val linePath = buildTrendLinePath(visiblePoints)
        val areaPath = buildTrendAreaPath(visiblePoints, baselineY)

        if (visiblePoints.size > 1) {
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

private fun buildTrendSeriesCatalog(): Map<Pair<TrendRangeOption, TrendMetricOption>, TrendSeriesData> {
    val cumulative = mapOf(
        TrendRangeOption.Last7Days to listOf(
            TrendPointData("6/23", "6/23", 126),
            TrendPointData("6/24", "6/24", 138),
            TrendPointData("6/25", "6/25", 152),
            TrendPointData("6/26", "6/26", 160),
            TrendPointData("6/27", "6/27", 166),
            TrendPointData("6/28", "6/28", 174),
            TrendPointData("今天", "今天", 188)
        ),
        TrendRangeOption.Last30Days to listOf(
            TrendPointData("5/31", "05/31", 82),
            TrendPointData("6/05", "06/05", 96),
            TrendPointData("6/10", "06/10", 118),
            TrendPointData("6/15", "06/15", 134),
            TrendPointData("6/20", "06/20", 158),
            TrendPointData("6/25", "06/25", 173),
            TrendPointData("今天", "今天", 188)
        ),
        TrendRangeOption.LastYear to listOf(
            TrendPointData("7 月", "7月", 24),
            TrendPointData("9 月", "9月", 51),
            TrendPointData("11 月", "11月", 79),
            TrendPointData("1 月", "1月", 103),
            TrendPointData("3 月", "3月", 137),
            TrendPointData("5 月", "5月", 166),
            TrendPointData("本月", "本月", 188)
        ),
        TrendRangeOption.ThisMonth to listOf(
            TrendPointData("6/01", "6/01", 92),
            TrendPointData("6/06", "6/06", 104),
            TrendPointData("6/11", "6/11", 121),
            TrendPointData("6/16", "6/16", 139),
            TrendPointData("6/21", "6/21", 159),
            TrendPointData("6/26", "6/26", 177),
            TrendPointData("今天", "今天", 188)
        ),
        TrendRangeOption.ThisYear to listOf(
            TrendPointData("1 月", "1月", 62),
            TrendPointData("2 月", "2月", 81),
            TrendPointData("3 月", "3月", 106),
            TrendPointData("4 月", "4月", 129),
            TrendPointData("5 月", "5月", 147),
            TrendPointData("6 月", "6月", 169),
            TrendPointData("本月", "本月", 188)
        )
    )

    val growth = mapOf(
        TrendRangeOption.Last7Days to listOf(
            TrendPointData("6/23", "6/23", 8),
            TrendPointData("6/24", "6/24", 12),
            TrendPointData("6/25", "6/25", 14),
            TrendPointData("6/26", "6/26", 9),
            TrendPointData("6/27", "6/27", 17),
            TrendPointData("6/28", "6/28", 11),
            TrendPointData("今天", "今天", 15)
        ),
        TrendRangeOption.Last30Days to listOf(
            TrendPointData("5/31", "05/31", 6),
            TrendPointData("6/05", "06/05", 10),
            TrendPointData("6/10", "06/10", 14),
            TrendPointData("6/15", "06/15", 9),
            TrendPointData("6/20", "06/20", 18),
            TrendPointData("6/25", "06/25", 12),
            TrendPointData("今天", "今天", 15)
        ),
        TrendRangeOption.LastYear to listOf(
            TrendPointData("7 月", "7月", 5),
            TrendPointData("9 月", "9月", 7),
            TrendPointData("11 月", "11月", 9),
            TrendPointData("1 月", "1月", 11),
            TrendPointData("3 月", "3月", 13),
            TrendPointData("5 月", "5月", 10),
            TrendPointData("本月", "本月", 15)
        ),
        TrendRangeOption.ThisMonth to listOf(
            TrendPointData("6/01", "6/01", 7),
            TrendPointData("6/06", "6/06", 9),
            TrendPointData("6/11", "6/11", 12),
            TrendPointData("6/16", "6/16", 10),
            TrendPointData("6/21", "6/21", 16),
            TrendPointData("6/26", "6/26", 13),
            TrendPointData("今天", "今天", 15)
        ),
        TrendRangeOption.ThisYear to listOf(
            TrendPointData("1 月", "1月", 6),
            TrendPointData("2 月", "2月", 8),
            TrendPointData("3 月", "3月", 11),
            TrendPointData("4 月", "4月", 9),
            TrendPointData("5 月", "5月", 14),
            TrendPointData("6 月", "6月", 12),
            TrendPointData("本月", "本月", 15)
        )
    )

    return buildMap {
        TrendRangeOption.entries.forEach { range ->
            put(range to TrendMetricOption.Cumulative, TrendSeriesData(cumulative.getValue(range)))
            put(range to TrendMetricOption.Growth, TrendSeriesData(growth.getValue(range)))
        }
    }
}

private fun formatTrendValue(metric: TrendMetricOption, value: Int): String {
    return when (metric) {
        TrendMetricOption.Cumulative -> "$value 词"
        TrendMetricOption.Growth -> "+$value 词"
    }
}

private fun buildVisibleTrendPoints(points: List<Offset>, revealProgress: Float): List<Offset> {
    if (points.isEmpty()) return emptyList()
    if (points.size == 1) return points
    if (revealProgress <= 0f) return listOf(points.first())
    if (revealProgress >= 1f) return points

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
