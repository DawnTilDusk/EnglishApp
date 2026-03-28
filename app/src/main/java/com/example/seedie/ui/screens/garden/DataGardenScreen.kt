package com.example.seedie.ui.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DataGardenScreen(
    viewModel: GardenViewModel = hiltViewModel()
) {
    val plots by viewModel.gardenPlots.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left side: Stats Panel (Takes 40% of width)
        StatsPanelSection(modifier = Modifier.weight(0.4f))

        // Right side: Garden Plot (Takes 60% of width)
        GardenPlotSection(
            modifier = Modifier.weight(0.6f),
            plots = plots,
            onPlotClick = { plot -> viewModel.onPlotClicked(plot) }
        )
    }
}