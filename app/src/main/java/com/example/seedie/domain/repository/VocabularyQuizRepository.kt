package com.example.seedie.domain.repository

import com.example.seedie.domain.quiz.VocabularyQuizConstants
import com.example.seedie.ui.screens.learning.quiz.VocabularyQuizSession

interface VocabularyQuizRepository {
    suspend fun createSession(
        sessionId: String,
        questionCount: Int = VocabularyQuizConstants.QUESTION_COUNT,
        difficulty: String = "mixed"
    ): VocabularyQuizSession
}
