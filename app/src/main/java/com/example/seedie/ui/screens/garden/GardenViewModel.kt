package com.example.seedie.ui.screens.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.entity.GardenPlotEntity
import com.example.seedie.domain.model.ActivityModuleSummary
import com.example.seedie.domain.model.DailyActivityTotal
import com.example.seedie.domain.model.activityModuleLabel
import com.example.seedie.domain.repository.ActivityTrackingRepository
import com.example.seedie.domain.usecase.GardenEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GardenModuleDurationUiState(
    val moduleId: String,
    val label: String,
    val durationSec: Int
)

data class GardenTrendPointUiState(
    val date: String,
    val label: String,
    val shortLabel: String,
    val durationSec: Int
)

data class GardenActivityStatsUiState(
    val totalActiveDurationSec: Int = 0,
    val moduleDurations: List<GardenModuleDurationUiState> = emptyList(),
    val trendPoints: List<GardenTrendPointUiState> = emptyList(),
    val hasActivity: Boolean = false,
    val emptyStateText: String = "今天还没有活跃记录，切换主页、学习页或商城后会开始本地累计。",
    val effectiveStudyHint: String = "这里展示的是全局活跃时长，不等同于练习结算中的学习有效时长。"
)

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val gardenEngine: GardenEngine,
    private val activityTrackingRepository: ActivityTrackingRepository
) : ViewModel() {

    private val clock: Clock = Clock.systemDefaultZone()

    val gardenPlots: StateFlow<List<GardenPlotEntity>> = gardenEngine.gardenPlots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = List(16) { index -> GardenPlotEntity(userId = "", plotIndex = index) }
        )

    val activityStats: StateFlow<GardenActivityStatsUiState> = combine(
        activityTrackingRepository.observeTodayModuleSummaries(),
        activityTrackingRepository.observeRecentDailyTotals(days = 7)
    ) { todayModules, recentTotals ->
        buildGardenActivityStats(
            todayModules = todayModules,
            recentTotals = recentTotals,
            clock = clock
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = buildGardenActivityStats(
            todayModules = emptyList(),
            recentTotals = emptyList(),
            clock = clock
        )
    )

    init {
        viewModelScope.launch {
            gardenEngine.initializeGarden()
        }
    }

    fun onPlotClicked(plot: GardenPlotEntity) {
        viewModelScope.launch {
            if (plot.plantType == "empty") {
                gardenEngine.plantSeed(plot.plotIndex)
            } else {
                gardenEngine.waterPlant(plot)
            }
        }
    }
}

internal fun buildGardenActivityStats(
    todayModules: List<ActivityModuleSummary>,
    recentTotals: List<DailyActivityTotal>,
    clock: Clock
): GardenActivityStatsUiState {
    val moduleDurations = todayModules
        .filter { it.durationSec > 0 }
        .sortedByDescending { it.durationSec }
        .map { summary ->
            GardenModuleDurationUiState(
                moduleId = summary.moduleId,
                label = activityModuleLabel(summary.moduleId),
                durationSec = summary.durationSec
            )
        }
    val totalActiveDurationSec = moduleDurations.sumOf { it.durationSec }
    val trendPoints = buildRecentTrendPoints(recentTotals, clock)
    return GardenActivityStatsUiState(
        totalActiveDurationSec = totalActiveDurationSec,
        moduleDurations = moduleDurations,
        trendPoints = trendPoints,
        hasActivity = totalActiveDurationSec > 0
    )
}

internal fun buildRecentTrendPoints(
    recentTotals: List<DailyActivityTotal>,
    clock: Clock
): List<GardenTrendPointUiState> {
    val formatter = DateTimeFormatter.ofPattern("M/d", Locale.getDefault())
    val today = LocalDate.now(clock)
    val totalMap = recentTotals.associateBy({ it.date }, { it.durationSec })
    return (6L downTo 0L).map { offset ->
        val date = today.minusDays(offset)
        val rawDate = date.toString()
        GardenTrendPointUiState(
            date = rawDate,
            label = if (date == today) "今天" else formatter.format(date),
            shortLabel = if (date == today) "今天" else formatter.format(date),
            durationSec = totalMap[rawDate] ?: 0
        )
    }
}
