package com.example.seedie.ui.screens.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.entity.GardenPlotEntity
import com.example.seedie.domain.model.ActivityModuleIds
import com.example.seedie.domain.model.ActivityModuleSummary
import com.example.seedie.domain.model.activityModuleLabel
import com.example.seedie.domain.repository.ActivityTrackingRepository
import com.example.seedie.domain.usecase.GardenEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val gardenEngine: GardenEngine,
    private val activityTrackingRepository: ActivityTrackingRepository
) : ViewModel() {

    val gardenPlots: StateFlow<List<GardenPlotEntity>> = gardenEngine.gardenPlots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = List(16) { index -> GardenPlotEntity(userId = "", plotIndex = index) }
        )

    val statsUiState: StateFlow<GardenStatsUiState> = activityTrackingRepository
        .observeTodayModuleSummaries()
        .map(::toGardenStatsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GardenStatsUiState()
        )

    init {
        viewModelScope.launch {
            gardenEngine.initializeGarden()
        }
    }

    fun onPlotClicked(plot: GardenPlotEntity) {
        viewModelScope.launch {
            if (plot.plantType == "empty") {
                // Try to plant a seed if empty
                gardenEngine.plantSeed(plot.plotIndex)
            } else {
                // Try to water/level up the plant
                gardenEngine.waterPlant(plot)
            }
        }
    }
}

data class GardenStatsUiState(
    val learningDistribution: LearningDistributionUiState = LearningDistributionUiState()
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
    ActivityModuleIds.QUIZ,
    ActivityModuleIds.LEARNING_HUB
)

private val learningDistributionLabels = mapOf(
    ActivityModuleIds.VOCABULARY_STUDY to "背单词",
    ActivityModuleIds.VOCABULARY_REVIEW to "单词复习",
    ActivityModuleIds.LISTENING to "听力训练",
    ActivityModuleIds.QUIZ to "词汇测验",
    ActivityModuleIds.LEARNING_HUB to "学习中心"
)

private fun toGardenStatsUiState(
    summaries: List<ActivityModuleSummary>
): GardenStatsUiState {
    val items = summaries
        .asSequence()
        .filter { it.durationSec > 0 }
        .filterNot { it.moduleId in excludedDistributionModuleIds }
        .sortedWith(
            compareByDescending<ActivityModuleSummary> { it.durationSec }
                .thenBy { preferredDistributionOrder.indexOf(it.moduleId).let { index -> if (index == -1) Int.MAX_VALUE else index } }
                .thenBy { learningDistributionLabels[it.moduleId] ?: activityModuleLabel(it.moduleId) }
        )
        .map { summary ->
            LearningDistributionItemUiState(
                moduleId = summary.moduleId,
                label = learningDistributionLabels[summary.moduleId] ?: activityModuleLabel(summary.moduleId),
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
