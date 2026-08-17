package com.example.seedie.ui.screens.garden

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
import java.time.format.DateTimeFormatter
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

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val rangeMode = MutableStateFlow(ForestRangeMode.Day)
    private val anchorDate = MutableStateFlow(todayString())
    private val selectedPlantId = MutableStateFlow<String?>(null)
    private val showGardenerHut = MutableStateFlow(false)
    private val vocabularyTrendPoints = MutableStateFlow<List<VocabularyTrendPointUiState>>(emptyList())
    private val vocabularyTrendRefreshTick = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            gardenEngine.ensureDefaultUnlocks()
        }
        refreshVocabularyTrend()
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

    val statsUiState: StateFlow<GardenStatsUiState> = combine(
        activityTrackingRepository.observeTodayModuleSummaries(),
        vocabularyTrendPoints,
        vocabularyTrendRefreshTick
    ) { summaries, trendPoints, refreshTick ->
        toGardenStatsUiState(summaries).copy(
            vocabularyTrendPoints = trendPoints,
            vocabularyTrendRefreshTick = refreshTick
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GardenStatsUiState()
    )

    fun refreshVocabularyTrend() {
        viewModelScope.launch {
            val points = runCatching { profileRepository.listMyVocabularyEstimates(limit = 30) }
                .getOrDefault(emptyList())
                .map(::toTrendPoint)
            vocabularyTrendPoints.value = points
            vocabularyTrendRefreshTick.update { it + 1 }
        }
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
    val vocabularyTrendRefreshTick: Int = 0
)

data class VocabularyTrendPointUiState(
    val label: String,
    val shortLabel: String,
    val value: Int
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

internal fun toTrendPoint(record: VocabularyEstimateRecord): VocabularyTrendPointUiState {
    val label = formatEstimateDateLabel(record.createdAt)
    return VocabularyTrendPointUiState(
        label = label,
        shortLabel = label,
        value = record.vocabularySize
    )
}

internal fun formatEstimateDateLabel(
    createdAt: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
    today: LocalDate = LocalDate.now(zoneId)
): String {
    val date = runCatching {
        Instant.parse(normalizeInstantString(createdAt)).atZone(zoneId).toLocalDate()
    }.getOrNull() ?: return createdAt.take(10)

    return if (date == today) "今天" else date.format(trendDateFormatter)
}

private fun normalizeInstantString(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.endsWith("Z") || trimmed.contains('+') ||
        Regex("""T\d{2}:\d{2}:\d{2}.*-\d{2}:\d{2}$""").containsMatchIn(trimmed)
    ) {
        return trimmed
    }
    return "${trimmed}Z"
}

private fun todayString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
