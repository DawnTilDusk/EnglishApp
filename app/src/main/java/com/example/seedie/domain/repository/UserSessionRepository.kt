package com.example.seedie.domain.repository

import com.example.seedie.domain.model.UserSessionState
import kotlinx.coroutines.flow.StateFlow

interface UserSessionRepository {
    val sessionState: StateFlow<UserSessionState>

    suspend fun addStudyTime(minutes: Int)

    /** @deprecated Vocabulary size is quiz estimate only; prefer [setVocabularyEstimate]. */
    suspend fun addVocabulary(count: Int)

    suspend fun setVocabularyEstimate(size: Int)
}
