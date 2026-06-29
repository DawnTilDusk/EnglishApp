package com.example.seedie.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.seedie.ui.screens.teacher.TeacherMainScreen
import com.example.seedie.ui.screens.teacher.dashboard.StudentDetailScreen

@Composable
fun TeacherNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = TeacherScreen.Main.route
    ) {
        composable(TeacherScreen.Main.route) {
            TeacherMainScreen(
                onStudentClick = { studentId ->
                    navController.navigate(TeacherScreen.StudentDetail.createRoute(studentId))
                }
            )
        }
        composable(
            route = TeacherScreen.StudentDetail.route,
            arguments = listOf(navArgument("studentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val studentId = backStackEntry.arguments?.getString("studentId") ?: return@composable
            StudentDetailScreen(
                studentId = studentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
