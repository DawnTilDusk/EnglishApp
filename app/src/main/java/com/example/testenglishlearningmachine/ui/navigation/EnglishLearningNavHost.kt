package com.example.testenglishlearningmachine.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.testenglishlearningmachine.ui.screens.HomeScreen
import com.example.testenglishlearningmachine.ui.screens.LearningScreen
import com.example.testenglishlearningmachine.ui.screens.ProfileScreen
import com.example.testenglishlearningmachine.ui.screens.QuizScreen

@Composable
fun EnglishLearningNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        composable(Screen.Learning.route) {
            LearningScreen(navController = navController)
        }
        composable(Screen.Quiz.route) {
            QuizScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
    }
}
