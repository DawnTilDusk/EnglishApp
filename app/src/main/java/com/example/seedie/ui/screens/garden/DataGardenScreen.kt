package com.example.seedie.ui.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DataGardenScreen(
    trendReplayKey: Int = 0,
    viewModel: GardenViewModel = hiltViewModel()
) {
    val forestUiState by viewModel.forestUiState.collectAsState()
    val statsUiState by viewModel.statsUiState.collectAsState()
    val removeMessage by viewModel.removeMessage.collectAsState()

    LaunchedEffect(trendReplayKey) {
        viewModel.refreshVocabularyTrend()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        StatsPanelSection(
            modifier = Modifier.weight(0.4f),
            learningDistribution = statsUiState.learningDistribution,
            vocabularyTrendPoints = statsUiState.vocabularyTrendPoints,
            trendReplayKey = trendReplayKey + statsUiState.vocabularyTrendRefreshTick,
            forestAliveCount = forestUiState.aliveCount,
            forestWitheredCount = forestUiState.witheredCount
        )

        ForestPanel(
            modifier = Modifier.weight(0.6f),
            state = forestUiState,
            onRangeModeChange = viewModel::setRangeMode,
            onShiftRange = viewModel::shiftRange,
            onTreeClick = viewModel::onTreeClick,
            onOpenGardenerHut = viewModel::openGardenerHut,
            onCloseGardenerHut = viewModel::closeGardenerHut,
            onRemoveWithered = viewModel::removeSelectedWitheredPlant,
            removeMessage = removeMessage,
            onClearRemoveMessage = viewModel::clearRemoveMessage
        )
    }
}
