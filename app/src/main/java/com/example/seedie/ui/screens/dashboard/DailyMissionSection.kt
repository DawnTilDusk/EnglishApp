package com.example.seedie.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.seedie.data.local.entity.DailyTaskEntity
import com.example.seedie.ui.components.TabSectionSurface

@Composable
fun DailyMissionSection(
    modifier: Modifier = Modifier,
    tasks: List<DailyTaskEntity> = listOf(
        DailyTaskEntity(id = 1, userId = "preview", date = "2024-01-01", title = "背诵 20 个单词", rewardAmount = 8),
        DailyTaskEntity(id = 2, userId = "preview", date = "2024-01-01", title = "完成一次语法测验", rewardAmount = 15, isCompleted = true, taskKey = "daily_vocabulary", rewardType = "dew", autoClaim = true),
        DailyTaskEntity(id = 3, userId = "preview", date = "2024-01-01", title = "提交一次作文", rewardAmount = 0, tokenReward = 10, autoClaim = true, rewardType = "token", taskKey = "daily_writing")
    )
) {
    TabSectionSurface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Layer
            Text(
                text = "每日任务",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            // List Layer
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
                    TaskCard(task = task)
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: DailyTaskEntity
) {
    val isTokenTask = task.tokenReward > 0
    val rewardValue = if (isTokenTask) task.tokenReward else task.rewardAmount
    val rewardLabel = if (isTokenTask) "代币" else "露水"
    val badgeColorTint = if (isTokenTask) Color(0xFFE6A23C) else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {},
            modifier = Modifier.size(24.dp),
            enabled = false
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = if (task.isCompleted) "Completed" else "Pending",
                tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                ),
                color = if (task.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            )
            if (!task.autoClaim && !task.isCompleted) {
                Text(
                    text = "完成后系统自动发放",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else if (task.autoClaim) {
                Text(
                    text = "自动发放",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Reward Badge Placeholder
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(badgeColorTint.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+$rewardValue $rewardLabel",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = badgeColorTint
            )
        }
    }
}
