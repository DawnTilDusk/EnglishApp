package com.example.seedie.domain.repository

import com.example.seedie.domain.model.UserSessionState
import kotlinx.coroutines.flow.StateFlow

interface UserSessionRepository {
    val sessionState: StateFlow<UserSessionState>
    
    suspend fun updateTokens(amount: Int)
    suspend fun addStudyTime(minutes: Int)
    suspend fun addVocabulary(count: Int)
}