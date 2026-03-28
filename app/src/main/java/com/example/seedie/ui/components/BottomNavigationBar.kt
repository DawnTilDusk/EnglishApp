package com.example.seedie.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.seedie.ui.theme.PrimaryGreen
import com.example.seedie.ui.theme.SecondaryBrown

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val pageIndex: Int
)

@Composable
fun BottomNavigationBar(
    currentPage: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        BottomNavItem("首页", Icons.Filled.Home, 0),
        BottomNavItem("学习", Icons.Filled.List, 1),
        BottomNavItem("数据", Icons.Filled.Star, 2),
        BottomNavItem("我的", Icons.Filled.Person, 3)
    )

    NavigationBar(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        contentColor = SecondaryBrown
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentPage == item.pageIndex,
                onClick = { onTabSelected(item.pageIndex) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryGreen,
                    selectedTextColor = PrimaryGreen,
                    unselectedIconColor = SecondaryBrown,
                    unselectedTextColor = SecondaryBrown,
                    indicatorColor = PrimaryGreen.copy(alpha = 0.1f)
                )
            )
        }
    }
}