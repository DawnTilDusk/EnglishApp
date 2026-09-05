package com.example.seedie.ui.screens.garden

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.ActivityModuleIds
import com.example.seedie.domain.model.ActivityModuleSummary
import com.example.seedie.domain.model.activityModuleLabel
import com.example.seedie.domain.repository.ActivityTrackingRepository
import com.example.seedie.domain.repository.ProfileRepository
import com.example.seedie.domain.repository.VocabularyEstimateRecord
import com.example.seedie.domain.usecase.ForestGridCell
import com.example.seedie.domain.usecase.ForestLayout
import com.example.seedie.domain.usecase.GardenEngine
import com.example.seedie.domain.usecase.PlacedTree
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

enum class ForestRangeMode { Day, Week }

data class ForestPanelUiState(
    val rangeMode: ForestRangeMode = ForestRangeMode.Day,
    val anchorDate: String = todayString(),
    val rangeLabel: String = todayString(),
    val cells: List<ForestGridCell> = ForestLayout.build(emptyList()).cells,
    val trees: List<PlacedTree> = emptyList(),
    val aliveCount: Int = 0,
    val witheredCount: Int = 0,
    val selectedTree: PlacedTree? = null,
    val showGardenerHut: Boolean = false
)

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val gardenEngine: GardenEngine,
    private val activityTrackingRepository: ActivityTrackingRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private companion object {
        const val VOCABULARY_TREND_LOG_TAG = "VocabularyTrend"
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val rangeMode = MutableStateFlow(ForestRangeMode.Day)
    private val anchorDate = MutableStateFlow(todayString())
    private val selectedPlantId = MutableStateFlow<String?>(null)
    private val showGardenerHut = MutableStateFlow(false)
    private val _removeMessage = MutableStateFlow<String?>(null)
    private val vocabularyTrendRange = MutableStateFlow(VocabularyTrendRange.Last7Days)
    private val vocabularyTrendMetric = MutableStateFlow(VocabularyTrendMetric.Estimate)
    private val vocabularyTrendPoints = MutableStateFlow<List<VocabularyTrendPointUiState>>(emptyList())
    private val vocabularyTrendRefreshTick = MutableStateFlow(0)
    private val vocabularyTrendLoadState = MutableStateFlow(VocabularyTrendLoadState())

    init {
        viewModelScope.launch {
            runCatching { gardenEngine.ensureDefaultUnlocks() }
                .onFailure { error -> Log.e("GardenViewModel", "ensureDefaultUnlocks failed", error) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val plantsFlow = combine(rangeMode, anchorDate) { mode, date ->
        mode to date
    }.flatMapLatest { (mode, date) ->
        when (mode) {
            ForestRangeMode.Day -> gardenEngine.observePlantsForDate(date)
            ForestRangeMode.Week -> {
                val (start, end) = weekBounds(date)
                gardenEngine.observePlantsBetween(start, end)
            }
        }
    }

    val forestUiState: StateFlow<ForestPanelUiState> = combine(
        plantsFlow,
        rangeMode,
        anchorDate,
        selectedPlantId,
        showGardenerHut
    ) { plants, mode, date, selectedId, hut ->
        val scene = ForestLayout.build(plants)
        ForestPanelUiState(
            rangeMode = mode,
            anchorDate = date,
            rangeLabel = when (mode) {
                ForestRangeMode.Day -> date
                ForestRangeMode.Week -> {
                    val (start, end) = weekBounds(date)
                    "$start ~ $end"
                }
            },
            cells = scene.cells,
            trees = scene.trees,
            aliveCount = plants.count { it.status == "ALIVE" },
            witheredCount = plants.count { it.status == "WITHERED" },
            selectedTree = scene.trees.firstOrNull { it.plantId == selectedId },
            showGardenerHut = hut
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ForestPanelUiState()
    )

    val removeMessage: StateFlow<String?> = _removeMessage

    private val vocabularyTrendContent = combine(
        vocabularyTrendPoints,
        vocabularyTrendRefreshTick,
        vocabularyTrendRange,
        vocabularyTrendMetric
    ) { trendPoints, refreshTick, trendRange, trendMetric ->
        VocabularyTrendContentState(
            vocabularyTrendPoints = trendPoints,
            vocabularyTrendRefreshTick = refreshTick,
            vocabularyTrendRange = trendRange,
            vocabularyTrendMetric = trendMetric
        )
    }

    val statsUiState: StateFlow<GardenStatsUiState> = combine(
        activityTrackingRepository.observeTodayModuleSummaries(),
        vocabularyTrendContent,
        vocabularyTrendLoadState
    ) { summaries, trendContent, loadState ->
        toGardenStatsUiState(summaries).copy(
            vocabularyTrendPoints = trendContent.vocabularyTrendPoints,
            vocabularyTrendRefreshTick = trendContent.vocabularyTrendRefreshTick,
            vocabularyTrendRange = trendContent.vocabularyTrendRange,
            vocabularyTrendMetric = trendContent.vocabularyTrendMetric,
            vocabularyTrendIsLoading = loadState.isLoading,
            vocabularyTrendErrorMessage = loadState.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GardenStatsUiState()
    )

    fun refreshVocabularyTrend() {
        val selectedRange = vocabularyTrendRange.value
        val selectedMetric = vocabularyTrendMetric.value
        val zoneId = ZoneId.systemDefault()
        val dateRange = selectedRange.resolveDateRange(LocalDate.now(zoneId))
        vocabularyTrendPoints.value = emptyList()
        vocabularyTrendLoadState.value = VocabularyTrendLoadState(isLoading = true)
        Log.i(
            VOCABULARY_TREND_LOG_TAG,
            "Refresh requested: range=$selectedRange, metric=$selectedMetric, " +
                "window=${dateRange.startDate}..${dateRange.endDateInclusive}, zone=$zoneId"
        )
        viewModelScope.launch {
            val result = runCatching {
                val records = profileRepository.listMyVocabularyTrendEstimates(
                    rangeStartInclusive = dateRange.startDate.atStartOfDay(zoneId).toInstant(),
                    rangeEndExclusive = dateRange.endDateInclusive
                        .plusDays(1)
                        .atStartOfDay(zoneId)
                        .toInstant()
                )
                records to buildVocabularyTrendPoints(
                    records = records,
                    range = selectedRange,
                    metric = selectedMetric,
                    dateRange = dateRange,
                    zoneId = zoneId
                )
            }
            if (
                vocabularyTrendRange.value == selectedRange &&
                vocabularyTrendMetric.value == selectedMetric
            ) {
                result.onSuccess { (records, points) ->
                    Log.i(
                        VOCABULARY_TREND_LOG_TAG,
                        "Refresh succeeded: remoteRecords=${records.size}, displayPoints=${points.size}, " +
                            "window=${dateRange.startDate}..${dateRange.endDateInclusive}"
                    )
                    vocabularyTrendPoints.value = points
                    vocabularyTrendLoadState.value = VocabularyTrendLoadState()
                    vocabularyTrendRefreshTick.update { it + 1 }
                }.onFailure { error ->
                    Log.e(
                        VOCABULARY_TREND_LOG_TAG,
                        "Refresh failed: window=${dateRange.startDate}..${dateRange.endDateInclusive}",
                        error
                    )
                    vocabularyTrendLoadState.value = VocabularyTrendLoadState(
                        errorMessage = "词汇量趋势加载失败，请点击重试"
                    )
                }
            }
        }
    }

    fun setVocabularyTrendRange(range: VocabularyTrendRange) {
        Log.i(VOCABULARY_TREND_LOG_TAG, "ViewModel received range selection=$range")
        if (vocabularyTrendRange.value != range) {
            vocabularyTrendRange.value = range
        }
        refreshVocabularyTrend()
    }

    fun setVocabularyTrendMetric(metric: VocabularyTrendMetric) {
        if (vocabularyTrendMetric.value != metric) {
            vocabularyTrendMetric.value = metric
        }
        refreshVocabularyTrend()
    }

    fun setRangeMode(mode: ForestRangeMode) {
        rangeMode.value = mode
    }

    fun shiftRange(delta: Int) {
        val cal = Calendar.getInstance()
        cal.time = dateFormat.parse(anchorDate.value) ?: Date()
        when (rangeMode.value) {
            ForestRangeMode.Day -> cal.add(Calendar.DAY_OF_YEAR, delta)
            ForestRangeMode.Week -> cal.add(Calendar.WEEK_OF_YEAR, delta)
        }
        anchorDate.value = dateFormat.format(cal.time)
        selectedPlantId.value = null
    }

    fun onTreeClick(tree: PlacedTree) {
        selectedPlantId.value = if (selectedPlantId.value == tree.plantId) null else tree.plantId
        _removeMessage.value = null
    }

    fun removeSelectedWitheredPlant() {
        val plantId = selectedPlantId.value ?: return
        viewModelScope.launch {
            when (gardenEngine.removeWitheredPlant(plantId)) {
                GardenEngine.RemoveWitheredResult.Success -> {
                    selectedPlantId.value = null
                    _removeMessage.value = null
                }
                GardenEngine.RemoveWitheredResult.InsufficientTokens -> {
                    _removeMessage.value = "代币不足，无法铲除枯苗"
                }
                GardenEngine.RemoveWitheredResult.NotFound,
                GardenEngine.RemoveWitheredResult.NotWithered -> {
                    _removeMessage.value = "这棵树现在不能铲除"
                }
                GardenEngine.RemoveWitheredResult.NotLoggedIn -> {
                    _removeMessage.value = "请先登录后再铲除"
                }
            }
        }
    }

    fun clearRemoveMessage() {
        _removeMessage.value = null
    }

    fun openGardenerHut() {
        showGardenerHut.value = true
    }

    fun closeGardenerHut() {
        showGardenerHut.value = false
    }

    private fun weekBounds(anchor: String): Pair<String, String> {
        val cal = Calendar.getInstance()
        cal.time = dateFormat.parse(anchor) ?: Date()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val end = dateFormat.format(cal.time)
        return start to end
    }
}

data class GardenStatsUiState(
    val learningDistribution: LearningDistributionUiState = LearningDistributionUiState(),
    val vocabularyTrendPoints: List<VocabularyTrendPointUiState> = emptyList(),
    val vocabularyTrendRefreshTick: Int = 0,
    val vocabularyTrendRange: VocabularyTrendRange = VocabularyTrendRange.Last7Days,
    val vocabularyTrendMetric: VocabularyTrendMetric = VocabularyTrendMetric.Estimate,
    val vocabularyTrendIsLoading: Boolean = false,
    val vocabularyTrendErrorMessage: String? = null
)

private data class VocabularyTrendContentState(
    val vocabularyTrendPoints: List<VocabularyTrendPointUiState>,
    val vocabularyTrendRefreshTick: Int,
    val vocabularyTrendRange: VocabularyTrendRange,
    val vocabularyTrendMetric: VocabularyTrendMetric
)

private data class VocabularyTrendLoadState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

enum class VocabularyTrendRange(val label: String) {
    Last7Days("近 7 天"),
    Last30Days("近 30 天"),
    LastYear("近 1 年"),
    ThisMonth("本月"),
    ThisYear("本年度");

    val usesMonthlySampling: Boolean
        get() = this == LastYear || this == ThisYear
}

enum class VocabularyTrendMetric(val label: String) {
    Estimate("词汇量估算"),
    MeasurementChange("检测区间变化")
}

enum class VocabularyTrendPointSource(val displayLabel: String) {
    ActualMeasurement("实际测评"),
    Interpolated("相邻测评估算"),
    CarriedForward("沿用最近测评")
}

internal data class VocabularyTrendDateRange(
    val startDate: LocalDate,
    val endDateInclusive: LocalDate
)

data class VocabularyTrendPointUiState(
    val date: LocalDate,
    val label: String,
    val shortLabel: String,
    val value: Int,
    val source: VocabularyTrendPointSource,
    val actualMeasurementCount: Int = 0
)

data class LearningDistributionUiState(
    val items: List<LearningDistributionItemUiState> = emptyList(),
    val totalDurationSec: Int = 0
) {
    val hasData: Boolean
        get() = items.isNotEmpty() && totalDurationSec > 0
}

data class LearningDistributionItemUiState(
    val moduleId: String,
    val label: String,
    val durationSec: Int
)

private val excludedDistributionModuleIds = setOf(
    ActivityModuleIds.DASHBOARD,
    ActivityModuleIds.DATA_GARDEN,
    ActivityModuleIds.PROFILE,
    ActivityModuleIds.SHOP
)

private val preferredDistributionOrder = listOf(
    ActivityModuleIds.VOCABULARY_STUDY,
    ActivityModuleIds.VOCABULARY_REVIEW,
    ActivityModuleIds.LISTENING,
    ActivityModuleIds.READING,
    ActivityModuleIds.QUIZ,
    ActivityModuleIds.LEARNING_HUB
)

private val learningDistributionLabels = mapOf(
    ActivityModuleIds.VOCABULARY_STUDY to "背单词",
    ActivityModuleIds.VOCABULARY_REVIEW to "单词复习",
    ActivityModuleIds.LISTENING to "听力训练",
    ActivityModuleIds.READING to "阅读训练",
    ActivityModuleIds.QUIZ to "词汇测验",
    ActivityModuleIds.LEARNING_HUB to "学习中心"
)

private val trendDateFormatter = DateTimeFormatter.ofPattern("M/d")

internal fun toGardenStatsUiState(
    summaries: List<ActivityModuleSummary>
): GardenStatsUiState {
    val items = summaries
        .asSequence()
        .filter { it.durationSec > 0 }
        .filterNot { it.moduleId in excludedDistributionModuleIds }
        .sortedWith(
            compareByDescending<ActivityModuleSummary> { it.durationSec }
                .thenBy {
                    preferredDistributionOrder.indexOf(it.moduleId)
                        .let { index -> if (index == -1) Int.MAX_VALUE else index }
                }
                .thenBy { learningDistributionLabels[it.moduleId] ?: activityModuleLabel(it.moduleId) }
        )
        .map { summary ->
            LearningDistributionItemUiState(
                moduleId = summary.moduleId,
                label = learningDistributionLabels[summary.moduleId]
                    ?: activityModuleLabel(summary.moduleId),
                durationSec = summary.durationSec
            )
        }
        .toList()

    return GardenStatsUiState(
        learningDistribution = LearningDistributionUiState(
            items = items,
            totalDurationSec = items.sumOf { it.durationSec }
        )
    )
}

internal fun VocabularyTrendRange.resolveDateRange(
    today: LocalDate
): VocabularyTrendDateRange {
    val start = when (this) {
        VocabularyTrendRange.Last7Days -> today.minusDays(6)
        VocabularyTrendRange.Last30Days -> today.minusDays(29)
        VocabularyTrendRange.LastYear -> today.minusYears(1).plusDays(1)
        VocabularyTrendRange.ThisMonth -> today.withDayOfMonth(1)
        VocabularyTrendRange.ThisYear -> today.withDayOfYear(1)
    }
    return VocabularyTrendDateRange(startDate = start, endDateInclusive = today)
}

private data class DailyVocabularyAnchor(
    val date: LocalDate,
    val value: Int,
    val recordId: String,
    val measurementCount: Int
)

/**
 * Builds a display-ready vocabulary trend from real quiz estimates.
 *
 * Each day with one or more measurements is a real anchor using that day's highest estimate.
 * Gaps between anchors are linearly interpolated with a deterministic micro-variation; dates
 * before the first anchor stay empty, while dates after the newest anchor carry it forward.
 */
internal fun buildVocabularyTrendPoints(
    records: List<VocabularyEstimateRecord>,
    range: VocabularyTrendRange,
    metric: VocabularyTrendMetric = VocabularyTrendMetric.Estimate,
    dateRange: VocabularyTrendDateRange,
    zoneId: ZoneId
): List<VocabularyTrendPointUiState> {
    val datedRecords = records.mapNotNull { record ->
        parseEstimateDate(record.createdAt, zoneId)?.let { date -> date to record }
    }
    val eligibleRecords = datedRecords
        .filter { (date, _) -> !date.isAfter(dateRange.endDateInclusive) }
    val anchors = eligibleRecords
        .groupBy({ it.first }, { it.second })
        .mapNotNull { (date, dailyRecords) ->
            val highest = dailyRecords.maxWithOrNull(
                compareBy<VocabularyEstimateRecord> { it.vocabularySize }
                    .thenBy { it.createdAt }
            ) ?: return@mapNotNull null
            DailyVocabularyAnchor(
                date = date,
                value = highest.vocabularySize,
                recordId = highest.id,
                measurementCount = dailyRecords.size
            )
        }
        .sortedBy { it.date }

    Log.i(
        "VocabularyTrend",
        "Trend transform: source=${records.size}, parsed=${datedRecords.size}, " +
            "eligible=${eligibleRecords.size}, anchors=${anchors.size}"
    )

    if (anchors.isEmpty()) return emptyList()

    if (metric == VocabularyTrendMetric.MeasurementChange) {
        val changePoints = anchors.mapIndexedNotNull { index, anchor ->
            if (anchor.date < dateRange.startDate || anchor.date > dateRange.endDateInclusive) {
                return@mapIndexedNotNull null
            }
            val previous = anchors.getOrNull(index - 1)
            anchor.date.toVocabularyTrendPoint(
                value = anchor.value - (previous?.value ?: anchor.value),
                source = VocabularyTrendPointSource.ActualMeasurement,
                actualMeasurementCount = anchor.measurementCount,
                today = dateRange.endDateInclusive
            )
        }
        return sampleVocabularyTrendPoints(changePoints, range)
    }

    val anchorsByDate = anchors.associateBy { it.date }
    val dailyPoints = buildList {
        var date = dateRange.startDate
        while (!date.isAfter(dateRange.endDateInclusive)) {
            val actual = anchorsByDate[date]
            when {
                actual != null -> add(
                    date.toVocabularyTrendPoint(
                        value = actual.value,
                        source = VocabularyTrendPointSource.ActualMeasurement,
                        actualMeasurementCount = actual.measurementCount,
                        today = dateRange.endDateInclusive
                    )
                )

                else -> {
                    val previous = anchors.lastOrNull { it.date.isBefore(date) }
                    val next = anchors.firstOrNull { it.date.isAfter(date) }
                    when {
                        previous != null && next != null -> add(
                            date.toVocabularyTrendPoint(
                                value = interpolateVocabularyValue(previous, next, date),
                                source = VocabularyTrendPointSource.Interpolated,
                                today = dateRange.endDateInclusive
                            )
                        )

                        previous != null -> add(
                            date.toVocabularyTrendPoint(
                                value = previous.value,
                                source = VocabularyTrendPointSource.CarriedForward,
                                today = dateRange.endDateInclusive
                            )
                        )

                        // Before the first ever measurement, there is no honest value to draw.
                        else -> Unit
                    }
                }
            }
            date = date.plusDays(1)
        }
    }

    return sampleVocabularyTrendPoints(dailyPoints, range)
}

private fun sampleVocabularyTrendPoints(
    points: List<VocabularyTrendPointUiState>,
    range: VocabularyTrendRange
): List<VocabularyTrendPointUiState> {
    if (!range.usesMonthlySampling) return points
    return points
        .groupBy { YearMonth.from(it.date) }
        .values
        .map { it.last() }
        .map { point ->
            point.copy(
                label = "${point.date.year}年${point.date.monthValue}月",
                shortLabel = "${point.date.monthValue}月"
            )
        }
}

private fun LocalDate.toVocabularyTrendPoint(
    value: Int,
    source: VocabularyTrendPointSource,
    today: LocalDate,
    actualMeasurementCount: Int = 0
): VocabularyTrendPointUiState {
    val label = if (this == today) "今天" else format(trendDateFormatter)
    return VocabularyTrendPointUiState(
        date = this,
        label = label,
        shortLabel = label,
        value = value,
        source = source,
        actualMeasurementCount = actualMeasurementCount
    )
}

private fun interpolateVocabularyValue(
    start: DailyVocabularyAnchor,
    end: DailyVocabularyAnchor,
    date: LocalDate
): Int {
    val totalDays = ChronoUnit.DAYS.between(start.date, end.date).coerceAtLeast(1)
    val elapsedDays = ChronoUnit.DAYS.between(start.date, date).coerceIn(0, totalDays)
    val progress = elapsedDays.toDouble() / totalDays
    val base = start.value + (end.value - start.value) * progress
    val valueDistance = abs(end.value - start.value)
    val amplitude = minOf(5.0, maxOf(1.0, valueDistance * 0.25))
    val envelope = sin(Math.PI * progress)
    val noise = stableNoise("${start.recordId}:${end.recordId}:$date:v1")
    val lowerBound = minOf(start.value, end.value)
    val upperBound = maxOf(start.value, end.value)
    return (base + noise * amplitude * envelope)
        .roundToInt()
        .coerceIn(lowerBound, upperBound)
}

private fun stableNoise(seed: String): Double {
    var hash = 1_125_899_906_842_597L
    seed.forEach { character ->
        hash = hash * 31 + character.code
    }
    val normalized = (hash and Long.MAX_VALUE).toDouble() / Long.MAX_VALUE.toDouble()
    return normalized * 2 - 1
}

private fun parseEstimateDate(createdAt: String, zoneId: ZoneId): LocalDate? {
    val normalized = normalizeInstantString(createdAt)
    return runCatching {
        Instant.parse(normalized).atZone(zoneId).toLocalDate()
    }.onFailure { error ->
        Log.w(
            "VocabularyTrend",
            "Unable to parse vocabulary estimate timestamp: raw=$createdAt, normalized=$normalized",
            error
        )
    }.getOrNull()
}

private fun normalizeInstantString(raw: String): String {
    val normalizedSeparator = raw.trim().replace(' ', 'T')
    val normalizedOffset = normalizedSeparator
        .replace(Regex("""([+-]\d{2})(\d{2})$"""), "${'$'}1:${'$'}2")
        .replace(Regex("""([+-]\d{2})$"""), "${'$'}1:00")
        // Android's Instant parser accepts UTC as "Z", but rejects PostgreSQL's
        // equivalent "+00:00" representation returned by the REST response.
        .replace(Regex("""[+-]00:00$"""), "Z")
    if (normalizedOffset.endsWith("Z") || normalizedOffset.contains('+') ||
        Regex("""T\d{2}:\d{2}:\d{2}.*-\d{2}:\d{2}$""").containsMatchIn(normalizedOffset)
    ) {
        return normalizedOffset
    }
    return "${normalizedOffset}Z"
}

private fun todayString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
