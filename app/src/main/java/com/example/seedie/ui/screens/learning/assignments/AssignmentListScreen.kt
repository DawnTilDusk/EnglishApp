package com.example.seedie.ui.screens.learning.assignments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.seedie.domain.model.PracticeAssignmentListItem
import com.example.seedie.domain.model.PracticeAssignmentMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssignmentListRoute(
    moduleId: String,
    onNavigateBack: () -> Unit,
    onOpenAssignment: (PracticeAssignmentArgs) -> Unit,
    viewModel: AssignmentListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(moduleId) {
        viewModel.initialize(moduleId)
    }

    DisposableEffect(lifecycleOwner, moduleId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AssignmentListScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onTabSelected = viewModel::onTabSelected,
        onRetry = viewModel::onRetry,
        onOpenItem = { item ->
            val mode = if (item.status == "submitted") {
                PracticeAssignmentMode.Review
            } else {
                PracticeAssignmentMode.Answer
            }
            onOpenAssignment(
                PracticeAssignmentArgs(
                    moduleId = item.moduleId,
                    submissionId = item.submissionId,
                    mode = mode
                )
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentListScreen(
    uiState: AssignmentListUiState,
    onNavigateBack: () -> Unit,
    onTabSelected: (AssignmentListTab) -> Unit,
    onRetry: () -> Unit,
    onOpenItem: (PracticeAssignmentListItem) -> Unit
) {
    val visible = when (uiState.selectedTab) {
        AssignmentListTab.Incomplete -> uiState.incomplete
        AssignmentListTab.Completed -> uiState.completed
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.moduleTitle) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.selectedTab == AssignmentListTab.Incomplete,
                    onClick = { onTabSelected(AssignmentListTab.Incomplete) },
                    icon = {},
                    label = { Text("未完成 (${uiState.incomplete.size})") }
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == AssignmentListTab.Completed,
                    onClick = { onTabSelected(AssignmentListTab.Completed) },
                    icon = {},
                    label = { Text("已完成 (${uiState.completed.size})") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(uiState.errorMessage ?: "加载失败")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRetry) { Text("重试") }
                    }
                }
                visible.isEmpty() -> {
                    Text(
                        text = when (uiState.selectedTab) {
                            AssignmentListTab.Incomplete ->
                                "老师还没有布置${uiState.moduleTitle}作业"
                            AssignmentListTab.Completed -> "暂无已完成的作业"
                        },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(visible, key = { it.submissionId }) { item ->
                            AssignmentCard(item = item, onClick = { onOpenItem(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentCard(
    item: PracticeAssignmentListItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${item.itemCount} 套 · 截止 ${formatTime(item.dueAtEpochMs)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (item.status == "submitted") {
            val score = if (item.totalCount > 0) {
                "正确 ${item.correctCount}/${item.totalCount}"
            } else {
                "已提交"
            }
            Text(score, style = MaterialTheme.typography.bodySmall)
        } else if (item.isOverdue) {
            Text("已逾期", color = Color(0xFFB91C1C), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "剩余 ${formatRemaining(item.dueAtEpochMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatTime(epochMs: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))
}

private fun formatRemaining(dueAtEpochMs: Long): String {
    val diff = dueAtEpochMs - System.currentTimeMillis()
    if (diff <= 0) return "即将截止"
    val minutes = diff / 60_000
    val days = minutes / (60 * 24)
    val hours = (minutes % (60 * 24)) / 60
    val mins = minutes % 60
    return when {
        days > 0 -> "${days}天${hours}小时"
        hours > 0 -> "${hours}小时${mins}分钟"
        else -> "${mins}分钟"
    }
}
