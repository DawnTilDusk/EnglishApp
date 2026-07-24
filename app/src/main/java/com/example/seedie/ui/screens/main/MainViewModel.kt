package com.example.seedie.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import com.example.seedie.domain.repository.UserSessionRepository
import com.example.seedie.domain.usecase.RewardEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    private val economyManager: EconomyManager,
    private val taskDao: DailyTaskDao,
    private val authService: AuthService,
    private val rewardEventBus: RewardEventBus,
    private val vocabularyPracticeRepository: VocabularyPracticeRepository
) : ViewModel() {

    private val handledSessions = mutableSetOf<String>()
    private val _vocabularyEntryState = MutableStateFlow(VocabularyEntryUiState())
    val vocabularyEntryState = _vocabularyEntryState.asStateFlow()

    init {
        refreshVocabularyEntryState()
    }

    fun handleStudyResult(result: StudyResult) {
        if (!handledSessions.add(result.sessionId)) return

        viewModelScope.launch {
            if (result.studyTimeMinutes > 0) {
                userSessionRepository.addStudyTime(result.studyTimeMinutes)
            }
            if (result.vocabularyDelta > 0) {
                userSessionRepository.addVocabulary(result.vocabularyDelta)
            }
            if (result.earnedTokens > 0) {
                val reason = when (result.moduleId) {
                    "listening" -> "Listening Practice"
                    "vocabulary_review" -> "Vocabulary Review"
                    "quiz" -> "Vocabulary Quiz"
                    else -> "Vocabulary Practice"
                }
                economyManager.addTokens(
                    amount = result.earnedTokens,
                    reason = reason,
                    refId = "study:${result.sessionId}"
                )
                rewardEventBus.emit(RewardEvent.TokenDropped(result.earnedTokens))
            }
            if (result.isCompleted) {
                when (result.moduleId) {
                    "listening" -> completeTodayListeningTask()
                    "quiz" -> Unit
                    else -> completeTodayVocabularyTask()
                }
            }
            refreshVocabularyEntryState()
        }
    }

    private suspend fun completeTodayListeningTask() {
        val userId = authService.currentSession.value?.userId ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val task = taskDao.getTasksByUserAndDate(userId, today)
            .first()
            .firstOrNull { !it.isCompleted && it.title.contains("听力") }
            ?: return

        taskDao.updateTask(task.copy(isCompleted = true))
    }

    private suspend fun completeTodayVocabularyTask() {
        val userId = authService.currentSession.value?.userId ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val task = taskDao.getTasksByUserAndDate(userId, today)
            .first()
            .firstOrNull { !it.isCompleted && (it.title.contains("背诵") || it.title.contains("单词")) }
            ?: return

        taskDao.updateTask(task.copy(isCompleted = true))
    }

    fun refreshVocabularyEntryState() {
        viewModelScope.launch {
            val pendingEntry = vocabularyPracticeRepository.getPendingReviewEntry()
            _vocabularyEntryState.value = VocabularyEntryUiState(
                pendingReviewCount = pendingEntry?.pendingWordCount ?: 0,
                pendingRoundId = pendingEntry?.roundId,
                shouldShowReviewBadge = (pendingEntry?.pendingWordCount ?: 0) > 0
            )
        }
    }
}
