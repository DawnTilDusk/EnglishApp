package com.example.seedie.domain.quiz

object VocabularyQuizConstants {
    const val MODULE_ID = "quiz"
    const val QUESTION_COUNT = 12
    const val WORDS_PER_DIFFICULTY = 4
    const val COMPLETION_BONUS = 5
    const val ESTIMATE_DISCLAIMER = "基于本次 12 题抽样估算，仅供参考"
    val DIFFICULTY_LEVELS = listOf("easy", "medium", "hard")
}
