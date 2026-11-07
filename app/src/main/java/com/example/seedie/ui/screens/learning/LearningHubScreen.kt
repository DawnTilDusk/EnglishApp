package com.example.seedie.ui.screens.learning

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.seedie.ui.components.GardenPageHeader
import kotlinx.coroutines.launch

@Composable
fun LearningHubScreen(modules: List<ModuleConfig>, onModuleClick: (ModuleConfig) -> Unit, snackbarHostState: SnackbarHostState) {
    val scope = rememberCoroutineScope()
    LazyVerticalGrid(columns = GridCells.Adaptive(300.dp), modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp), horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            GardenPageHeader("让好奇心，慢慢长大", "选择今天的学习方向，按自己的节奏前进。", Modifier.padding(bottom = 8.dp), "LEARNING / 学习中心")
        }
        items(modules, key = { it.id }) { module ->
            StudyModuleCard(module, onClick = {
                if (module.isAvailable) onModuleClick(module)
                else scope.launch { snackbarHostState.showSnackbar("正在准备，敬请期待") }
            })
        }
    }
}
