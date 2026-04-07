package com.example.seedie.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeRoute
import com.example.seedie.ui.screens.main.MainScreen
import com.example.seedie.ui.screens.splash.SplashScreen

@Composable
fun SeedieNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    var pendingStudyResult by remember { mutableStateOf<StudyResult?>(null) }

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
                onOpenVocabulary = {
                    navController.navigate(Screen.VocabularyPractice.route)
                },
                pendingStudyResult = pendingStudyResult,
                onStudyResultConsumed = {
                    pendingStudyResult = null
                }
            )
        }
        composable(route = Screen.VocabularyPractice.route) {
            VocabularyPracticeRoute(
                args = VocabularyPracticeArgs(sourceModuleId = "vocabulary"),
                onFinishSession = { result ->
                    pendingStudyResult = result
                    navController.popBackStack()
                }
            )
        }
    }
}
