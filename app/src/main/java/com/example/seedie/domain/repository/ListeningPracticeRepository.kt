package com.example.seedie.domain.repository

import com.example.seedie.ui.screens.learning.listening.ListeningPracticeSession

interface ListeningPracticeRepository {
    suspend fun createSession(
        sessionId: String,
        itemRefs: List<String>? = null
    ): ListeningPracticeSession
}
