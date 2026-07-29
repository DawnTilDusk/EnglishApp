package com.example.seedie.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.seedie.domain.model.ActivityModule
import com.example.seedie.domain.usecase.GlobalActivityTracker
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeRoute
import com.example.seedie.ui.screens.learning.listening.ListeningPracticeRoute
import com.example.seedie.ui.screens.learning.quiz.VocabularyQuizRoute
import com.example.seedie.ui.screens.learning.reading.ReadingPracticeRoute
import com.example.seedie.ui.screens.main.MainScreen
import com.example.seedie.ui.screens.shop.MyOrdersScreen
import com.example.seedie.ui.screens.shop.StudentShopScreen
import com.example.seedie.ui.screens.splash.SplashScreen

@Composable
fun SeedieNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route,
    activityTracker: GlobalActivityTracker
) {
    var pendingStudyResult by remember { mutableStateOf<StudyResult?>(null) }
    var currentVocabularyArgs by remember { mutableStateOf(VocabularyPracticeArgs(sourceModuleId = "vocabulary")) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    LaunchedEffect(currentRoute, currentVocabularyArgs) {
        val module = when (currentRoute) {
            Screen.VocabularyPractice.route -> ActivityModule.fromPracticeSource(currentVocabularyArgs.sourceModuleId)
            Screen.ListeningPractice.route -> ActivityModule.ListeningPractice
            Screen.ReadingPractice.route -> ActivityModule.ReadingPractice
            Screen.VocabularyQuiz.route -> ActivityModule.VocabularyQuiz
            Screen.StudentShop.route,
            Screen.MyOrders.route -> ActivityModule.Shop
            Screen.Main.route -> null
            else -> null
        }
        if (currentRoute != Screen.Main.route) {
            activityTracker.trackModule(module)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(route = Screen.Main.route) {
            MainScreen(
                onOpenVocabularyStudy = { args ->
                    currentVocabularyArgs = args
                    navController.navigate(Screen.VocabularyPractice.route)
                },
                onOpenVocabularyReview = { args ->
                    currentVocabularyArgs = args
                    navController.navigate(Screen.VocabularyPractice.route)
                },
                onOpenListeningPractice = {
                    navController.navigate(Screen.ListeningPractice.route)
                },
                onOpenReadingPractice = {
                    navController.navigate(Screen.ReadingPractice.route)
                },
                onOpenVocabularyQuiz = {
                    navController.navigate(Screen.VocabularyQuiz.route)
                },
                onOpenShop = {
                    navController.navigate(Screen.StudentShop.route)
                },
                onVisibleModuleChanged = activityTracker::trackModule,
                pendingStudyResult = pendingStudyResult,
                onStudyResultConsumed = {
                    pendingStudyResult = null
                }
            )
        }
        composable(route = Screen.VocabularyPractice.route) {
            VocabularyPracticeRoute(
                args = currentVocabularyArgs,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFinishSession = { result ->
                    pendingStudyResult = result
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.ListeningPractice.route) {
            ListeningPracticeRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFinishSession = { result ->
                    pendingStudyResult = result
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.ReadingPractice.route) {
            ReadingPracticeRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFinishSession = { result ->
                    pendingStudyResult = result
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.VocabularyQuiz.route) {
            VocabularyQuizRoute(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onFinishSession = { result ->
                    pendingStudyResult = result
                    navController.popBackStack()
                }
            )
        }
        composable(route = Screen.StudentShop.route) {
            StudentShopScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenOrders = { navController.navigate(Screen.MyOrders.route) }
            )
        }
        composable(route = Screen.MyOrders.route) {
            MyOrdersScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
