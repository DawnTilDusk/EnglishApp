package com.example.seedie.domain.repository

import com.example.seedie.ui.screens.learning.reading.ReadingPracticeSession

interface ReadingPracticeRepository {
    suspend fun createSession(sessionId: String): ReadingPracticeSession
}
