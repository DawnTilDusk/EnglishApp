package com.example.seedie.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Main : Screen("main")
    object VocabularyPractice : Screen("vocabulary_practice")
    object ListeningPractice : Screen("listening_practice")
    object ReadingPractice : Screen("reading_practice")
    object VocabularyQuiz : Screen("vocabulary_quiz")
    object StudentShop : Screen("student_shop")
    object MyOrders : Screen("my_orders")
    object ReadingAssignments : Screen("reading_assignments")
    object ListeningAssignments : Screen("listening_assignments")
    object WritingHub : Screen("writing_hub")
    object WritingAssignments : Screen("writing_assignments")
    object WritingPractice : Screen("writing_practice")
    object WritingSentenceTranslation : Screen("writing_sentence_translation")
    object WritingConnectorDrill : Screen("writing_connector_drill")
    object WritingParaphrase : Screen("writing_paraphrase")
    object ReadingMode : Screen("reading_mode")
    object ListeningMode : Screen("listening_mode")
    object ReadingCatalog : Screen("reading_catalog")
    object ListeningCatalog : Screen("listening_catalog")
}
