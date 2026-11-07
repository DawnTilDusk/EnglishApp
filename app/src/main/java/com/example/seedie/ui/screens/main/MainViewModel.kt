package com.example.seedie.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.ProfileRepository
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import com.example.seedie.domain.repository.UserSessionRepository
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VocabularyEntryUiState(
    val defaultSubtitle: String = "滚动学习队列，继续今天的新词",
    val pendingReviewCount: Int = 0,
    val pendingRoundId: String? = null,
    val shouldShowReviewBadge: Boolean = false
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val vocabularyPracticeRepository: VocabularyPracticeRepository,
    private val profileRepository: ProfileRepository,
    private val settlements: com.example.seedie.data.repository.StudySettlementManager
) : ViewModel() {

    private val _vocabularyEntryState = MutableStateFlow(VocabularyEntryUiState())
    val vocabularyEntryState = _vocabularyEntryState.asStateFlow()

    init {
        refreshVocabularyEntryState()
        hydrateVocabularyEstimate()
    }

    suspend fun handleStudyResult(result: StudyResult): String {
        refreshVocabularyEntryState()
        result.estimatedVocabulary?.let { userSessionRepository.setVocabularyEstimate(it) }
        return settlements.rewardSummary(result)
    }

    private fun hydrateVocabularyEstimate() {
        viewModelScope.launch {
            runCatching { profileRepository.getMyProfile() }
                .onSuccess { profile ->
                    if (profile.hasVocabularyEstimate) {
                        userSessionRepository.setVocabularyEstimate(profile.vocabularySize)
                    }
                }
        }
    }

    fun refreshVocabularyEntryState() {
        viewModelScope.launch {
            val pendingEntry = runCatching {
                vocabularyPracticeRepository.getPendingReviewEntry()
            }.onFailure { error ->
                Log.e("MainViewModel", "Failed to load pending vocabulary review", error)
            }.getOrNull()
            _vocabularyEntryState.value = VocabularyEntryUiState(
                pendingReviewCount = pendingEntry?.pendingWordCount ?: 0,
                pendingRoundId = pendingEntry?.roundId,
                shouldShowReviewBadge = (pendingEntry?.pendingWordCount ?: 0) > 0
            )
        }
    }
}
