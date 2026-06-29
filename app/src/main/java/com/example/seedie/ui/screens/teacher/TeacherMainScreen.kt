package com.example.seedie.ui.screens.teacher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.seedie.ui.components.TeacherBottomNavigationBar
import com.example.seedie.ui.screens.teacher.dashboard.TeacherDashboardScreen
import com.example.seedie.ui.screens.teacher.shop.TeacherShopScreen

@Composable
fun TeacherMainScreen(
    onStudentClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    Scaffold(
        bottomBar = {
            TeacherBottomNavigationBar(pagerState = pagerState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "教师工作台",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> TeacherDashboardScreen(onStudentClick = onStudentClick)
                    1 -> TeacherShopScreen()
                }
            }
        }
    }
}
