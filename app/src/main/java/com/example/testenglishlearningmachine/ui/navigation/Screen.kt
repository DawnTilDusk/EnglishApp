package com.example.testenglishlearningmachine.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Learning : Screen("learning")
    object Quiz : Screen("quiz")
    object Profile : Screen("profile")
}
