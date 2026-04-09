package com.example.seedie.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.seedie.ui.screens.login.LoginScreen

@Composable
fun SeedieNavGraph(
    onLoginSuccess: () -> Unit = {},
    navController: NavHostController = rememberNavController()
) {
    // 这个专门处理“未登录”状态的路由体系，起点是 "login"
    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onNavigateToNext = {
                    /*
                     * 🌟🌟🌟 【跳转接口 - 新手引导逻辑注入点】 🌟🌟🌟
                     *
                     * 因为我们在 MainActivity 里监听了 AuthService.currentSession，
                     * 当 LoginViewModel 成功登录并写入 session 时，
                     * MainActivity 会瞬间收到通知 (isLoggedIn = true)，
                     * 并且自动把整个界面切到你写的 `SeedieNavHost` (也就是 SplashScreen 签到页)。
                     * 
                     * 所以在这里，如果老用户登录成功，你【什么都不用写】，系统会自动切走。
                     * 
                     * 但如果你以后做了【新手引导】，你可以改成这样：
                     * val isNewUser = true // (通过 ViewModel 或数据库查)
                     * if (isNewUser) {
                     *     navController.navigate("guide") // 不触发全局切换，而是跳到当前路由的新手引导页
                     * } else {
                     *     // 老用户：直接完成登录逻辑，MainActivity 会自动接管切去签到页
                     * }
                     */
                    onLoginSuccess()
                }
            )
        }

        // 将来的新手引导页 (占位)
        // composable("guide") {
        //     GuideScreen(
        //         onFinishGuide = { 
        //             // 引导完成，标记不是新用户了，触发全局刷新进入签到页
        //         }
        //     )
        // }
    }
}
