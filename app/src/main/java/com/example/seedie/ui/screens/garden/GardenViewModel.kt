package com.example.seedie.ui.screens.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.entity.GardenPlotEntity
import com.example.seedie.domain.usecase.GardenEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GardenViewModel @Inject constructor(
    private val gardenEngine: GardenEngine
) : ViewModel() {

    val gardenPlots: StateFlow<List<GardenPlotEntity>> = gardenEngine.gardenPlots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = List(16) { GardenPlotEntity(it) } // Default empty
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