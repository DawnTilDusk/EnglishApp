package com.example.seedie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.seedie.ui.SeedieNavGraph
import com.example.seedie.ui.navigation.SeedieNavHost
import com.example.seedie.ui.theme.SeedieTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // 使用 Hilt 获取 MainViewModel
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeedieTheme {
                // 监听登录状态
                val isLoggedIn by mainViewModel.isLoggedIn.collectAsState()

                // 根据状态决定显示哪个“路由根节点”
                when (isLoggedIn) {
                    null -> {
                        // 还在检查缓存中的 Token，显示一个全屏加载动画
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    true -> {
                        // 【已登录】：直接进入你的主业务流程 (SeedieNavHost)，第一站是你的 SplashScreen 签到页
                        SeedieNavHost()
                    }
                    false -> {
                        // 【未登录】：进入专门处理登录、注册、新手引导的流程 (SeedieNavGraph)
                        SeedieNavGraph(
                            onLoginSuccess = {
                            }
                        )
                    }
                }
            }
        }
    }
}
