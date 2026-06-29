package com.example.seedie.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import kotlinx.coroutines.launch

@Composable
fun TeacherBottomNavigationBar(
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf(
        Pair("学生数据", Icons.AutoMirrored.Filled.List),
        Pair("商城", Icons.Default.ShoppingCart)
    )

    NavigationBar(
        modifier = modifier.fillMaxWidth(),
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
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.surface,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.secondary,
                    unselectedTextColor = MaterialTheme.colorScheme.secondary
                )
            )
        }
    }
}
