package com.example.seedie.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BottomNavigationBar(pagerState: PagerState, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val tabs = listOf("首页" to Icons.Default.Home, "学习" to Icons.Default.School,
        "花园" to Icons.Default.Park, "我的" to Icons.Default.PersonOutline)
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .65f))) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Row(Modifier.widthIn(max = 720.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                tabs.forEachIndexed { index, (label, icon) ->
                    val selected = pagerState.currentPage == index
                    Surface(Modifier.weight(1f), shape = MaterialTheme.shapes.medium,
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
                        Row(Modifier.selectable(selected = selected, role = Role.Tab, onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }).heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally)) {
                            val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                            Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
                        }
                    }
                }
            }
        }
    }
}
