package com.example.seedie.domain.repository

import com.example.seedie.ui.screens.learning.listening.ListeningPracticeSession

interface ListeningPracticeRepository {
    suspend fun createSession(
        questionCount: Int = 10,
        difficulty: String = "mixed",
        sessionId: String
    ): ListeningPracticeSession
}
