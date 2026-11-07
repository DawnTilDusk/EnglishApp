package com.example.seedie.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
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
    viewModel: DashboardViewModel = hiltViewModel(),
    onOpenTask: (String) -> Unit = {}
) {
    val tasks by viewModel.dailyTasks.collectAsState()
    val activityDays by viewModel.activityDays.collectAsState()
    val writingMessage by viewModel.writingMessage.collectAsState()
    com.example.seedie.ui.components.PracticeLifecycle { active ->
        if (active) viewModel.refreshWriting()
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        com.example.seedie.ui.components.GardenPageHeader("每一天，都有一点进步", "完成今天的小目标，让学习慢慢长成习惯。")
        Spacer(Modifier.height(18.dp))
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            DashboardFeatureCarousel(modifier = Modifier.weight(.56f), activityDays = activityDays)
            DailyMissionSection(
                modifier = Modifier.weight(.44f), tasks = tasks,
                onOpenTask = { key -> viewModel.recordTaskOpen(key); onOpenTask(key) },
                writingMessage = writingMessage, onRefreshWriting = viewModel::refreshWriting
            )
        }
    }
}
