package com.example.seedie.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.seedie.data.local.entity.DailyTaskEntity
import com.example.seedie.ui.components.TabSectionSurface

@Composable
fun DailyMissionSection(
    modifier: Modifier = Modifier,
    tasks: List<DailyTaskEntity> = emptyList(),
    onOpenTask: (String) -> Unit = {},
    writingMessage: String = "",
    onRefreshWriting: () -> Unit = {}
) {
    TabSectionSurface(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("今天的小目标", style = MaterialTheme.typography.titleLarge)
                Text("${tasks.count { it.isCompleted }} / ${tasks.size}", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tasks, key = { it.id }) { task -> TaskCard(task, onOpenTask) }
                if (writingMessage.isNotBlank()) item {
                    TextButton(onClick = onRefreshWriting) {
                        Text(writingMessage, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(task: DailyTaskEntity, onOpenTask: (String) -> Unit = {}) {
    Surface(color = if (task.isCompleted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .32f)
        else MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(task.title, style = MaterialTheme.typography.titleSmall)
                    Text(if (task.isCompleted) "今日目标已完成" else if (task.ruleVersion >= 2)
                        "${task.currentCount.coerceAtMost(task.targetCount)} / ${task.targetCount} · 一点点积累" else "旧版任务 · 次日启用新规则",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!task.isCompleted) FilledTonalButton(onClick = { onOpenTask(task.taskKey) },
                    shape = MaterialTheme.shapes.small, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(if (task.currentCount > 0) "继续" else "开始")
                } else Text("已完成", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (task.ruleVersion >= 2) LinearProgressIndicator(
                progress = { (task.currentCount.toFloat() / task.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(4.dp), trackColor = MaterialTheme.colorScheme.surfaceVariant)
            val amount = if (task.tokenReward > 0) task.tokenReward else task.rewardAmount
            Text(when {
                task.isCompleted && !task.rewardGranted -> "奖励待处理"
                task.isCompleted -> "奖励已处理，可在个人中心查看"
                task.rewardType == "dew" -> "最多 +$amount 露珠 · 受每日上限限制 · 本地保存"
                else -> "+$amount 代币 · 联网后同步"
            }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
