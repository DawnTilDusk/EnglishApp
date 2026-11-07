package com.example.seedie.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun DashboardFeatureCarousel(modifier: Modifier = Modifier, activityDays: List<com.example.seedie.domain.model.DailyActivityTotal> = emptyList()) {
    val pager = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("学习足迹", "今日一句").forEachIndexed { index, label ->
                FilterChip(selected = pager.currentPage == index,
                    onClick = { scope.launch { pager.animateScrollToPage(index) } }, label = { Text(label) })
            }
            Spacer(Modifier.weight(1f))
            Text("${pager.currentPage + 1} / 2", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), pageSpacing = 16.dp) { page ->
            if (page == 0) HeatmapSection(Modifier.fillMaxSize(), activityDays) else DailyQuoteSection(Modifier.fillMaxSize())
        }
    }
}
