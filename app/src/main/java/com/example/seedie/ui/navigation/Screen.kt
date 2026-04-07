package com.example.seedie.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Main : Screen("main")
    object VocabularyPractice : Screen("vocabulary_practice")
}
