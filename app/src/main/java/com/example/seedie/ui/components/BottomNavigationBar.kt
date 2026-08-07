package com.example.seedie.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.foundation.pager.PagerState

@Composable
fun BottomNavigationBar(
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    val tabs = listOf(
        Pair("首页", Icons.Default.Home),
        Pair("学习", Icons.AutoMirrored.Filled.List),
        Pair("数据", Icons.Default.Star),
        Pair("我的", Icons.Default.Person)
    )

    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        // 容器底：奶油白（surface），让底栏与卡片保持同一层材质
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = pagerState.currentPage == index
            NavigationBarItem(
                selected = selected,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                icon = { Icon(imageVector = tab.second, contentDescription = tab.first) },
                label = { Text(tab.first) },
                // 选中态：primaryContainer（Green100 新芽白绿）做胶囊底，
                // 前景使用 onPrimaryContainer（Green800 深林绿），比原深绿反白更轻盈；
                // 未选中：onSurfaceVariant（Green700 苍林绿）保持可读性并统一到绿色系
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}