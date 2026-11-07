package com.example.seedie.ui.screens.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
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

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        com.example.seedie.ui.components.GardenPageHeader("看见自己的成长", "每一次认真学习，都会在这里留下痕迹。", eyebrow = "YOUR GARDEN / 成长花园")
        Spacer(Modifier.height(18.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        StatsPanelSection(
            modifier = Modifier.weight(0.4f),
            learningDistribution = statsUiState.learningDistribution,
            vocabularyTrendPoints = statsUiState.vocabularyTrendPoints,
            vocabularyTrendRange = statsUiState.vocabularyTrendRange,
            vocabularyTrendMetric = statsUiState.vocabularyTrendMetric,
            vocabularyTrendIsLoading = statsUiState.vocabularyTrendIsLoading,
            vocabularyTrendErrorMessage = statsUiState.vocabularyTrendErrorMessage,
            trendReplayKey = trendReplayKey + statsUiState.vocabularyTrendRefreshTick,
            onVocabularyTrendRangeChange = viewModel::setVocabularyTrendRange,
            onVocabularyTrendMetricChange = viewModel::setVocabularyTrendMetric,
            onRetryVocabularyTrend = viewModel::refreshVocabularyTrend
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
}
