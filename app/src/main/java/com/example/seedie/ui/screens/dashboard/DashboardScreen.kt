package com.example.seedie.ui.screens.dashboard

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
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val tasks by viewModel.dailyTasks.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left side: Heatmap (takes 60% of width)
        HeatmapSection(modifier = Modifier.weight(0.6f))

        // Right side: Daily Missions (takes 40% of width)
        DailyMissionSection(
            modifier = Modifier.weight(0.4f),
            tasks = tasks,
            onTaskClick = { task -> viewModel.onTaskClicked(task) }
        )
    }
}