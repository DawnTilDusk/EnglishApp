package com.example.seedie.ui.screens.garden

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.seedie.ui.components.TabSectionSurface
import com.example.seedie.ui.theme.AccentOrange
import com.example.seedie.ui.theme.ForestDeep
import com.example.seedie.ui.theme.MossGreen
import com.example.seedie.ui.theme.OliveYellow
import com.example.seedie.ui.theme.PrimaryGreen
import com.example.seedie.ui.theme.SageGreen
import com.example.seedie.ui.theme.SecondaryBrown
import com.example.seedie.ui.theme.gardenPressable
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private data class DonutSliceData(
    val label: String,
    val durationSec: Int,
    val color: Color
)

private data class TrendPointData(
    val date: java.time.LocalDate,
    val label: String,
    val shortLabel: String,
    val value: Int,
    val source: VocabularyTrendPointSource,
    val actualMeasurementCount: Int
)

@Composable
fun StatsPanelSection(
    modifier: Modifier = Modifier,
    learningDistribution: LearningDistributionUiState = LearningDistributionUiState(),
    vocabularyTrendPoints: List<VocabularyTrendPointUiState> = emptyList(),
    vocabularyTrendRange: VocabularyTrendRange = VocabularyTrendRange.Last7Days,
    vocabularyTrendMetric: VocabularyTrendMetric = VocabularyTrendMetric.Estimate,
    vocabularyTrendIsLoading: Boolean = false,
    vocabularyTrendErrorMessage: String? = null,
    trendReplayKey: Int = 0,
    onVocabularyTrendRangeChange: (VocabularyTrendRange) -> Unit = {},
    onVocabularyTrendMetricChange: (VocabularyTrendMetric) -> Unit = {},
    onRetryVocabularyTrend: () -> Unit = {}
) {
    val donutData = remember(learningDistribution.items) {
        learningDistribution.items.mapIndexed { index, item ->
            DonutSliceData(
                label = item.label,
                durationSec = item.durationSec,
                color = donutPaletteColor(index)
            )
        }
    }
    val trendPoints = remember(vocabularyTrendPoints) {
        vocabularyTrendPoints.map { point ->
            TrendPointData(
                date = point.date,
                label = point.label,
                shortLabel = point.shortLabel,
                value = point.value,
                source = point.source,
                actualMeasurementCount = point.actualMeasurementCount
            )
        }
    }

    var selectedDonutIndex by remember { mutableStateOf<Int?>(null) }
    var trendRefreshKey by remember { mutableIntStateOf(0) }
    var selectedTrendIndex by remember(trendPoints) {
        mutableIntStateOf(trendPoints.lastIndex.coerceAtLeast(0))
    }

    LaunchedEffect(trendReplayKey) {
        if (trendReplayKey > 0) {
            trendRefreshKey += 1
        }
    }

    LaunchedEffect(donutData) {
        if (selectedDonutIndex !in donutData.indices) {
            selectedDonutIndex = null
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
            distribution = learningDistribution,
            slices = donutData,
            selectedIndex = selectedDonutIndex,
            replayKey = trendReplayKey,
            onSelectionChange = { tappedIndex ->
                selectedDonutIndex = if (selectedDonutIndex == tappedIndex) null else tappedIndex
            }
        )

        TrendFocusCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            points = trendPoints,
            selectedRange = vocabularyTrendRange,
            selectedMetric = vocabularyTrendMetric,
            isLoading = vocabularyTrendIsLoading,
            errorMessage = vocabularyTrendErrorMessage,
            selectedIndex = selectedTrendIndex,
            refreshKey = trendRefreshKey,
            onSelectionChange = { selectedTrendIndex = it },
            onRangeChange = onVocabularyTrendRangeChange,
            onMetricChange = onVocabularyTrendMetricChange,
            onRetry = onRetryVocabularyTrend
        )
    }
}

@Composable
private fun DonutFocusCard(
    modifier: Modifier = Modifier,
    distribution: LearningDistributionUiState,
    slices: List<DonutSliceData>,
    selectedIndex: Int?,
    replayKey: Int,
    onSelectionChange: (Int) -> Unit
) {
    val cardShape = RoundedCornerShape(28.dp)
    val totalDurationSec = distribution.totalDurationSec
    val selectedSlice = selectedIndex?.let(slices::getOrNull)
    val leadingSlice = slices.maxByOrNull { it.durationSec }
    var detailsExpanded by rememberSaveable { mutableStateOf(true) }
    val hasDetails = detailsExpanded && distribution.hasData
    val legendScrollState = rememberScrollState()
    var chartHostSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val chartWidthFraction by animateFloatAsState(
        targetValue = if (hasDetails) 0.52f else 0.84f,
        animationSpec = tween(durationMillis = 260),
        label = "DonutChartWidthFraction"
    )
    val chartOffsetX by animateDpAsState(
        targetValue = if (hasDetails && chartHostSize.width > 0) {
            with(density) { (chartHostSize.width * -0.23f).toDp() }
        } else {
            0.dp
        },
        animationSpec = tween(durationMillis = 260),
        label = "DonutChartOffsetX"
    )
    val summaryText = when {
        !distribution.hasData -> "今天还没有学习记录，开始学习后这里会自动更新。"
        selectedSlice != null -> "你今天在${selectedSlice.label}上投入了${formatDurationShort(selectedSlice.durationSec)}。"
        leadingSlice != null -> "今天已学习${formatDurationShort(totalDurationSec)}，${leadingSlice.label}占比最高。"
        else -> "今天的学习分布会在这里自动整理。"
    }

    LaunchedEffect(distribution.hasData) {
        detailsExpanded = distribution.hasData
    }

    TabSectionSurface(
        modifier = modifier,
        shape = cardShape,
        accentColor = PrimaryGreen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "学习时间分布",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(enabled = distribution.hasData) {
                            if (distribution.hasData) {
                                detailsExpanded = !detailsExpanded
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (distribution.hasData) {
                        PrimaryGreen.copy(alpha = if (detailsExpanded) 0.16f else 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    }
                ) {
                    Text(
                        text = when {
                            !distribution.hasData -> "等待记录"
                            detailsExpanded -> "收起分类"
                            else -> "详细分类"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (distribution.hasData) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { chartHostSize = it }
            ) {
                DonutChart(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(chartWidthFraction)
                        .align(Alignment.Center)
                        .offset(x = chartOffsetX),
                    slices = slices,
                    selectedIndex = selectedIndex,
                    totalDurationSec = totalDurationSec,
                    refreshKey = replayKey,
                    onSliceSelected = onSelectionChange
                )

                androidx.compose.animation.AnimatedVisibility(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.46f)
                        .align(Alignment.TopEnd),
                    visible = hasDetails,
                    enter = fadeIn(animationSpec = tween(180)) +
                        slideInVertically(
                            animationSpec = tween(220),
                            initialOffsetY = { -it / 5 }
                        ),
                    exit = fadeOut(animationSpec = tween(160)) +
                        slideOutVertically(
                            animationSpec = tween(180),
                            targetOffsetY = { -it / 6 }
                        )
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.00f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(legendScrollState)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            slices.forEachIndexed { index, slice ->
                                LegendListItem(
                                    label = slice.label,
                                    supporting = buildSliceSupportingText(
                                        durationSec = slice.durationSec,
                                        totalDurationSec = totalDurationSec
                                    ),
                                    color = slice.color,
                                    selected = index == selectedIndex,
                                    onClick = { onSelectionChange(index) }
                                )
                                if (index != slices.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.68f)
                                    )
                                }
                            }
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
    points: List<TrendPointData>,
    selectedRange: VocabularyTrendRange,
    selectedMetric: VocabularyTrendMetric,
    isLoading: Boolean,
    errorMessage: String?,
    selectedIndex: Int,
    refreshKey: Int,
    onSelectionChange: (Int) -> Unit,
    onRangeChange: (VocabularyTrendRange) -> Unit,
    onMetricChange: (VocabularyTrendMetric) -> Unit,
    onRetry: () -> Unit
) {
    val cardShape = RoundedCornerShape(28.dp)
    var rangeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var metricMenuExpanded by rememberSaveable { mutableStateOf(false) }

    TabSectionSurface(
        modifier = modifier,
        shape = cardShape,
        accentColor = AccentOrange
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "词汇量趋势",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { rangeMenuExpanded = true },
                        shape = RoundedCornerShape(16.dp),
                        color = AccentOrange.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = selectedRange.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AccentOrange
                        )
                    }
                    DropdownMenu(
                        expanded = rangeMenuExpanded,
                        onDismissRequest = { rangeMenuExpanded = false }
                    ) {
                        VocabularyTrendRange.entries.forEach { range ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = range.label,
                                        color = if (range == selectedRange) {
                                            AccentOrange
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    rangeMenuExpanded = false
                                    onRangeChange(range)
                                }
                            )
                        }
                    }
                }
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { metricMenuExpanded = true },
                        shape = RoundedCornerShape(16.dp),
                        color = AccentOrange.copy(alpha = 0.10f)
                    ) {
                        Text(
                            text = selectedMetric.label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AccentOrange
                        )
                    }
                    DropdownMenu(
                        expanded = metricMenuExpanded,
                        onDismissRequest = { metricMenuExpanded = false }
                    ) {
                        VocabularyTrendMetric.entries.forEach { metric ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = metric.label,
                                        color = if (metric == selectedMetric) {
                                            AccentOrange
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    metricMenuExpanded = false
                                    onMetricChange(metric)
                                }
                            )
                        }
                    }
                }
            }

            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> Text(
                            text = "正在加载词汇量趋势…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        errorMessage != null -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "重新加载",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(onClick = onRetry)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = AccentOrange
                            )
                        }

                        else -> Text(
                            text = "当前范围内暂无词汇测评记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val safeSelectedIndex = selectedIndex.coerceIn(points.indices)
                LineChartSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    points = points,
                    selectedIndex = safeSelectedIndex,
                    metric = selectedMetric,
                    refreshKey = refreshKey,
                    onSelectionChange = onSelectionChange
                )
            }
        }
    }
}

@Composable
private fun DonutChart(
    modifier: Modifier = Modifier,
    slices: List<DonutSliceData>,
    selectedIndex: Int?,
    totalDurationSec: Int,
    refreshKey: Int,
    onSliceSelected: (Int) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val colorScheme = MaterialTheme.colorScheme
    val trackColor = colorScheme.surfaceVariant.copy(alpha = 0.32f)
    val centerSurfaceColor = colorScheme.surface.copy(alpha = 0.98f)
    val onSurface = colorScheme.onSurface
    val safeTotalDurationSec = totalDurationSec.coerceAtLeast(1)
    val revealProgress = remember { Animatable(0f) }

    LaunchedEffect(refreshKey, slices) {
        revealProgress.snapTo(0f)
        revealProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 980)
        )
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
            val baseStroke = minDimension * 0.18f
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
                val fullSweep = slice.durationSec / safeTotalDurationSec.toFloat() * 360f
                val sweepAngle = fullSweep * revealProgress.value
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
                    .size(114.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = selectedIndex, label = "DonutCenterText") { currentIndex ->
                    val currentSlice = currentIndex?.let(slices::getOrNull)
                    val headline = currentSlice?.let { formatDurationCompact(it.durationSec) }
                        ?: formatDurationCompact(totalDurationSec)
                    val title = when {
                        currentSlice != null -> currentSlice.label
                        totalDurationSec > 0 -> "今日总时长"
                        else -> "开始后更新"
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
                    }
                }
            }
        }
    }
}

// 学习时间分布环形图配色：前 3 位仍是 Seedie 三主色（生机绿/暖阳橙/橡木棕），
// 保证「最主要的切片依然是 IP 主色」；后续用新增补充色继续丰富层次，
// 让不同学习模块像同一片花园里的不同植物，色相彼此协调不刺眼。
private val donutPalette = listOf(
    PrimaryGreen,   // 生机绿：主色，占比最高的模块用它承担品牌情绪
    AccentOrange,   // 暖阳橙：次强调，第二大切片保持 IP 的暖色亮点
    SecondaryBrown, // 橡木棕：中性木质色，第三切片撑起层次
    MossGreen,      // 苔藓绿：偏黄的中绿，形成绿色内部的自然过渡
    SageGreen,      // 鼠尾草绿：柔和过渡色，让相邻切片衔接更顺
    OliveYellow,    // 橄榄黄绿：暖调收获色，用于成就/复盘类
    ForestDeep      // 深森林绿：最深切片，压住整体不飘
)

private fun donutPaletteColor(index: Int): Color {
    return donutPalette[index % donutPalette.size]
}

private fun buildSliceSupportingText(durationSec: Int, totalDurationSec: Int): String {
    if (durationSec <= 0 || totalDurationSec <= 0) return "0%"
    val percent = (durationSec * 100f / totalDurationSec).roundToInt()
    return "${formatDurationShort(durationSec)} · $percent%"
}

private fun formatDurationCompact(durationSec: Int): String {
    if (durationSec <= 0) return "0 min"
    val totalMinutes = (durationSec + 59) / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours <= 0 -> "${totalMinutes} min"
        minutes == 0 -> "${hours} h"
        else -> "${hours} h ${minutes} min"
    }
}

private fun formatDurationShort(durationSec: Int): String {
    return formatDurationCompact(durationSec)
}

@Composable
private fun LegendListItem(
    label: String,
    supporting: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) color.copy(alpha = 0.08f) else Color.Transparent)
            .gardenPressable(shape = MaterialTheme.shapes.small, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = supporting,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun LineChartSection(
    modifier: Modifier = Modifier,
    points: List<TrendPointData>,
    selectedIndex: Int,
    metric: VocabularyTrendMetric,
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
    val sourceDescription = if (metric == VocabularyTrendMetric.MeasurementChange) {
        "相对上一次真实测评"
    } else when (selectedPoint.source) {
        VocabularyTrendPointSource.ActualMeasurement -> {
            if (selectedPoint.actualMeasurementCount > 1) {
                "实际测评 · 当日 ${selectedPoint.actualMeasurementCount} 次取最高值"
            } else {
                "实际测评"
            }
        }
        VocabularyTrendPointSource.Interpolated -> "相邻测评估算 · 含轻微稳定波动"
        VocabularyTrendPointSource.CarriedForward -> "沿用最近一次测评"
    }
    val valueLabel = if (metric == VocabularyTrendMetric.MeasurementChange) {
        val prefix = if (selectedPoint.value > 0) "+" else ""
        "${selectedPoint.label} · $prefix${selectedPoint.value} 词"
    } else {
        "${selectedPoint.label} · ${selectedPoint.value} 词"
    }
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
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = valueLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = PrimaryGreen
                            )
                            Text(
                                text = sourceDescription,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val labelStep = when {
                points.size <= 8 -> 1
                points.size <= 16 -> 2
                points.size <= 31 -> 5
                else -> 1
            }
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
                        text = if (index % labelStep == 0 || index == points.lastIndex) {
                            point.shortLabel
                        } else {
                            ""
                        },
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
        }

        val revealX = visiblePoints.last().x
        chartPoints.zipWithNext().forEachIndexed { index, (start, end) ->
            if (start.x > revealX) return@forEachIndexed
            val visibleEnd = if (end.x <= revealX) {
                end
            } else {
                val fraction = ((revealX - start.x) / (end.x - start.x)).coerceIn(0f, 1f)
                Offset(
                    x = revealX,
                    y = start.y + (end.y - start.y) * fraction
                )
            }
            val linksTwoActualMeasurements =
                points[index].source == VocabularyTrendPointSource.ActualMeasurement &&
                    points[index + 1].source == VocabularyTrendPointSource.ActualMeasurement
            drawLine(
                color = PrimaryGreen.copy(alpha = if (linksTwoActualMeasurements) 1f else 0.72f),
                start = start,
                end = visibleEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (linksTwoActualMeasurements) null else {
                    PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))
                }
            )
        }

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
            when (points[index].source) {
                VocabularyTrendPointSource.ActualMeasurement -> drawCircle(
                    color = if (isSelected) AccentOrange else PrimaryGreen,
                    radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
                    center = point
                )

                VocabularyTrendPointSource.Interpolated -> drawCircle(
                    color = if (isSelected) AccentOrange else PrimaryGreen.copy(alpha = 0.76f),
                    radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
                    center = point,
                    style = Stroke(width = 2.dp.toPx())
                )

                VocabularyTrendPointSource.CarriedForward -> drawCircle(
                    color = if (isSelected) AccentOrange else SageGreen,
                    radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
                    center = point,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
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
    val strokeWidth = minDimension * 0.18f
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
    val total = slices.sumOf { it.durationSec }.toFloat()
    if (total <= 0f) return null
    var currentSweep = 0f
    slices.forEachIndexed { index, slice ->
        val sliceSweep = slice.durationSec / total * 360f
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

    val minValue = min(points.minOf { it.value }, 0)
    val maxValue = max(points.maxOf { it.value }, 1)
    val valueRange = (maxValue - minValue).coerceAtLeast(1)
    val usableHeight = max(chartSize.height - topPadding - bottomPadding, 1f)
    val usableWidth = max(chartSize.width - horizontalPadding * 2f, 1f)
    val stepX = if (points.size == 1) 0f else usableWidth / (points.size - 1)

    return points.mapIndexed { index, point ->
        val ratio = (point.value - minValue) / valueRange.toFloat()
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
