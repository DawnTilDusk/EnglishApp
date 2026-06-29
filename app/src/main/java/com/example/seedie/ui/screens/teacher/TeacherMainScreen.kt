package com.example.seedie.ui.screens.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.seedie.ui.components.LogoutConfirmDialog
import com.example.seedie.ui.components.TeacherBottomNavigationBar
import com.example.seedie.ui.screens.teacher.dashboard.TeacherDashboardScreen
import com.example.seedie.ui.screens.teacher.shop.TeacherShopScreen

@Composable
fun TeacherMainScreen(
    onStudentClick: (String) -> Unit,
    viewModel: TeacherMainViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    var showLogoutDialog by remember { mutableStateOf(false) }

    LogoutConfirmDialog(
        visible = showLogoutDialog,
        onConfirm = {
            showLogoutDialog = false
            viewModel.logout()
        },
        onDismiss = { showLogoutDialog = false }
    )

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "教师工作台",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(onClick = { showLogoutDialog = true }) {
                    Text("退出登录")
                }
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
