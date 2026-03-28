package com.example.seedie.domain.usecase

import com.example.seedie.data.local.dao.GardenPlotDao
import com.example.seedie.data.local.entity.GardenPlotEntity
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.repository.EconomyManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GardenEngine @Inject constructor(
    private val gardenPlotDao: GardenPlotDao,
    private val economyManager: EconomyManager,
    private val rewardEventBus: RewardEventBus
) {
    val gardenPlots: Flow<List<GardenPlotEntity>> = gardenPlotDao.getAllPlots()

    suspend fun initializeGarden() {
        val plots = List(16) { index ->
            // Seed a few initial plants for the MVP look
            when (index) {
                5 -> GardenPlotEntity(index, "flower", 1)
                6 -> GardenPlotEntity(index, "grass", 0)
                9 -> GardenPlotEntity(index, "flower", 2)
                10 -> GardenPlotEntity(index, "grass", 1)
                else -> GardenPlotEntity(index, "empty", 0)
            }
        }
        gardenPlotDao.initializePlots(plots)
    }

    suspend fun plantSeed(plotIndex: Int, type: String = "flower"): Boolean {
        // Cost 20 tokens to plant a seed
        if (economyManager.spendTokens(20, "Planted $type seed")) {
            val newPlot = GardenPlotEntity(plotIndex, type, 0)
            gardenPlotDao.updatePlot(newPlot)
            return true
        }
        return false
    }

    suspend fun waterPlant(plot: GardenPlotEntity): Boolean {
        // Cost 10 tokens to level up a plant
        if (plot.plantType == "empty" || plot.level >= 2) return false
        
        if (economyManager.spendTokens(10, "Watered plant at ${plot.plotIndex}")) {
            val upgradedPlot = plot.copy(level = plot.level + 1)
            gardenPlotDao.updatePlot(upgradedPlot)
            rewardEventBus.emit(RewardEvent.PlantLeveledUp(plot.plotIndex, upgradedPlot.level))
            return true
        }
        return false
    }
}