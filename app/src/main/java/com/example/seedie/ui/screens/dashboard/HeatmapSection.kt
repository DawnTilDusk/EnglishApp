package com.example.seedie.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.seedie.domain.model.DailyActivityTotal
import com.example.seedie.ui.components.TabSectionSurface
import java.time.LocalDate

@Composable
fun HeatmapSection(modifier: Modifier = Modifier, activityDays: List<DailyActivityTotal> = emptyList()) {
    val today = LocalDate.now()
    val byDate = activityDays.associate { it.date to it.durationSec }
    val active = activityDays.count { it.durationSec > 0 }
    TabSectionSurface(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("活动日历", style = MaterialTheme.typography.titleLarge)
                    Text("近 35 天 · 记录每一次打开与投入", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    Text("$active 个活跃日", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(35) { index ->
                    val date = today.minusDays((34-index).toLong())
                    val seconds = byDate[date.toString()] ?: 0
                    val intensity = if (seconds == 0) 0f else (.22f + seconds / 3600f).coerceAtMost(1f)
                    val color = lerp(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primary, intensity)
                    Box(Modifier.fillMaxWidth().aspectRatio(1.45f).clip(MaterialTheme.shapes.extraSmall).background(color)
                        .semantics { contentDescription = "${date.monthValue}月${date.dayOfMonth}日，应用活动 ${seconds / 60} 分钟" },
                        contentAlignment = Alignment.Center) {
                        Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelSmall,
                            color = if (intensity > .55f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text(if (active == 0) "从今天开始，为日常添一点绿。" else "本机应用活动记录 · 颜色越深，停留时间越长。",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
