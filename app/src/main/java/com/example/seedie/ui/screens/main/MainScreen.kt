package com.example.seedie.ui.screens.main

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.seedie.ui.components.BottomNavigationBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import com.example.seedie.ui.screens.learning.LearningHubScreen
import com.example.seedie.ui.screens.learning.ModuleConfig
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.School
import com.example.seedie.ui.screens.dashboard.DashboardScreen
import com.example.seedie.ui.screens.profile.ProfileScreen
import com.example.seedie.ui.components.CustomIndicatorPanel

@Composable
fun MainScreen() {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val snackbarHostState = remember { SnackbarHostState() }

    val learningModules = listOf(
        ModuleConfig("vocab", "背单词", "今日还剩 20 词", Icons.AutoMirrored.Filled.MenuBook, isAvailable = true, hasNewContent = true),
        ModuleConfig("grammar", "语法", "句型结构突破", Icons.Default.School),
        ModuleConfig("quiz", "词汇测验", "检验学习成果", Icons.Default.Quiz),
        ModuleConfig("textbook", "教材训练", "同步课堂进度", Icons.Default.Book),
        ModuleConfig("listening", "听力训练", "磨耳朵", Icons.Default.Headphones),
        ModuleConfig("writing", "写作/专项", "句型实战", Icons.Default.Create)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Column {
                // Suspended indicator above bottom bar
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CustomIndicatorPanel(pagerState = pagerState)
                }
                BottomNavigationBar(pagerState = pagerState)
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) { page ->
            when (page) {
                0 -> DashboardScreen() // Tab 1: Dashboard
                1 -> LearningHubScreen(
                    modules = learningModules,
                    onModuleClick = { /* TODO: Route to specific study page */ },
                    snackbarHostState = snackbarHostState
                ) // Tab 2: Learning Hub
                3 -> ProfileScreen() // Tab 4: Profile & Rewards
                else -> {
                    // Placeholder for other Tabs
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tab ${page + 1}",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}