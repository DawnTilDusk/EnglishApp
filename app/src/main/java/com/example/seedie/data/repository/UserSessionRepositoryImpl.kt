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

    // Study stats only. Token balance is owned exclusively by EconomyManager.
    private val _sessionState = MutableStateFlow(UserSessionState())
    override val sessionState: StateFlow<UserSessionState> = _sessionState.asStateFlow()

    override suspend fun addStudyTime(minutes: Int) {
        _sessionState.update { it.copy(totalStudyTimeMinutes = it.totalStudyTimeMinutes + minutes) }
    }

    @Deprecated("Vocabulary size is quiz estimate only")
    override suspend fun addVocabulary(count: Int) {
        // No-op: practice mastery must not inflate vocabulary size.
    }

    override suspend fun setVocabularyEstimate(size: Int) {
        _sessionState.update {
            it.copy(
                vocabularySize = size.coerceAtLeast(0),
                hasVocabularyEstimate = true
            )
        }
    }
}
