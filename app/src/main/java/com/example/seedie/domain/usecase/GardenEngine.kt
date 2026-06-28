package com.example.seedie.domain.usecase

import com.example.seedie.data.local.dao.GardenPlotDao
import com.example.seedie.data.local.entity.GardenPlotEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.repository.EconomyManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GardenEngine @Inject constructor(
    private val gardenPlotDao: GardenPlotDao,
    private val economyManager: EconomyManager,
    private val rewardEventBus: RewardEventBus,
    private val authService: AuthService
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val gardenPlots: Flow<List<GardenPlotEntity>> = authService.currentSession
        .flatMapLatest { session ->
            gardenPlotDao.getAllPlots(session?.userId ?: "")
        }

    suspend fun initializeGarden() {
        val userId = authService.currentSession.value?.userId ?: ""
        val plots = List(16) { index ->
            when (index) {
                5 -> GardenPlotEntity(userId = userId, plotIndex = index, plantType = "flower", level = 1)
                6 -> GardenPlotEntity(userId = userId, plotIndex = index, plantType = "grass", level = 0)
                9 -> GardenPlotEntity(userId = userId, plotIndex = index, plantType = "flower", level = 2)
                10 -> GardenPlotEntity(userId = userId, plotIndex = index, plantType = "grass", level = 1)
                else -> GardenPlotEntity(userId = userId, plotIndex = index)
            }
        }
        gardenPlotDao.initializePlots(plots)
    }

    suspend fun plantSeed(plotIndex: Int, type: String = "flower"): Boolean {
        if (economyManager.spendTokens(20, "Planted $type seed")) {
            val userId = authService.currentSession.value?.userId ?: ""
            val newPlot = GardenPlotEntity(userId = userId, plotIndex = plotIndex, plantType = type, level = 0)
            gardenPlotDao.updatePlot(newPlot)
            return true
        }
        return false
    }

    suspend fun waterPlant(plot: GardenPlotEntity): Boolean {
        if (plot.plantType == "empty" || plot.level >= 2) return false
        if (economyManager.spendTokens(10, "Watered plant at ${plot.plotIndex}")) {
            val upgradedPlot = plot.copy(level = plot.level + 1, syncStatus = "PENDING", syncedAt = null)
            gardenPlotDao.updatePlot(upgradedPlot)
            rewardEventBus.emit(RewardEvent.PlantLeveledUp(plot.plotIndex, upgradedPlot.level))
            return true
        }
        return false
    }
}
