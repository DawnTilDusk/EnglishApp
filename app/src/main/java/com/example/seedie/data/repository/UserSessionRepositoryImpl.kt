package com.example.seedie.data.repository

import com.example.seedie.domain.model.UserSessionState
import com.example.seedie.domain.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionRepositoryImpl @Inject constructor() : UserSessionRepository {

    // For MVP, we hold session state in memory. 
    // In a real app, this would be backed by DataStore or Room and initialized on startup.
    private val _sessionState = MutableStateFlow(UserSessionState())
    override val sessionState: StateFlow<UserSessionState> = _sessionState.asStateFlow()

    override suspend fun addTokens(amount: Int) {
        _sessionState.update { it.copy(tokens = it.tokens + amount) }
    }

    override suspend fun addStudyTime(minutes: Int) {
        _sessionState.update { it.copy(totalStudyTimeMinutes = it.totalStudyTimeMinutes + minutes) }
    }

    override suspend fun addVocabulary(count: Int) {
        _sessionState.update { it.copy(vocabularySize = it.vocabularySize + count) }
    }
}