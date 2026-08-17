package com.example.seedie.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.DewManager
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.ProfileRepository
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import com.example.seedie.domain.repository.UserSessionRepository
import com.example.seedie.domain.usecase.GardenEngine
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
    private val dewManager: DewManager,
    private val taskDao: DailyTaskDao,
    private val authService: AuthService,
    private val rewardEventBus: RewardEventBus,
    private val vocabularyPracticeRepository: VocabularyPracticeRepository,
    private val profileRepository: ProfileRepository,
    private val gardenEngine: GardenEngine
) : ViewModel() {

    private val handledSessions = mutableSetOf<String>()
    private val _vocabularyEntryState = MutableStateFlow(VocabularyEntryUiState())
    val vocabularyEntryState = _vocabularyEntryState.asStateFlow()

    init {
        refreshVocabularyEntryState()
        hydrateVocabularyEstimate()
    }

    fun handleStudyResult(result: StudyResult) {
        if (!handledSessions.add(result.sessionId)) return

        viewModelScope.launch {
            if (result.studyTimeMinutes > 0) {
                userSessionRepository.addStudyTime(result.studyTimeMinutes)
            }
            result.estimatedVocabulary?.let { estimate ->
                userSessionRepository.setVocabularyEstimate(estimate)
                profileRepository.setMyVocabularyEstimate(estimate)
            }
            if (result.earnedTokens > 0) {
                val reason = when (result.moduleId) {
                    "listening" -> "Listening Practice"
                    "reading" -> "Reading Practice"
                    "writing" -> "Writing Practice"
                    "vocabulary_review" -> "Vocabulary Review"
                    "quiz" -> "Vocabulary Quiz"
                    else -> "Vocabulary Practice"
                }
                economyManager.addTokens(
                    amount = result.earnedTokens,
                    reason = reason,
                    refId = when (result.moduleId) {
                        "reading", "listening", "writing" ->
                            if (result.sessionId.startsWith("free_")) {
                                result.sessionId
                            } else {
                                "assignment:${result.sessionId}"
                            }
                        else -> "study:${result.sessionId}"
                    }
                )
                rewardEventBus.emit(RewardEvent.TokenDropped(result.earnedTokens))
            }
            if (result.earnedDews > 0) {
                val reason = when (result.moduleId) {
                    "listening" -> "Listening Dew"
                    "reading" -> "Reading Dew"
                    "writing" -> "Writing Dew"
                    "quiz" -> "Quiz Dew"
                    else -> "Vocabulary Dew"
                }
                dewManager.addDews(
                    amount = result.earnedDews,
                    reason = reason,
                    refId = "dew:study:${result.sessionId}"
                )
                rewardEventBus.emit(RewardEvent.DewDropped(result.earnedDews))
            }
            gardenEngine.recordFromStudyResult(result)
            if (result.isCompleted) {
                handleCompletedTasks(result)
            }
            refreshVocabularyEntryState()
        }
    }

    private suspend fun handleCompletedTasks(result: StudyResult) {
        val userId = authService.currentSession.value?.userId ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val tasks = taskDao.getTasksByUserAndDate(userId, today).first()
        val taskKeys = when (result.moduleId) {
            "listening" -> listOf("daily_listening", "daily_completed_2")
            "reading" -> listOf("daily_reading", "daily_completed_2")
            "writing" -> listOf("daily_writing", "daily_completed_2")
            "quiz" -> listOf("daily_completed_2")
            else -> listOf("daily_vocabulary", "daily_completed_1", "daily_completed_2")
        }
        taskKeys.forEach { taskKey ->
            val match = tasks.firstOrNull { !it.isCompleted && it.taskKey == taskKey } ?: return@forEach
            autoClaimTask(userId, today, match)
        }
    }

    private suspend fun autoClaimTask(
        userId: String,
        today: String,
        task: com.example.seedie.data.local.entity.DailyTaskEntity
    ) {
        if (!task.autoClaim) return
        val updated = task.copy(isCompleted = true, completedAt = System.currentTimeMillis())
        taskDao.updateTask(updated)
        if (task.rewardType == "dew" && task.rewardAmount > 0) {
            dewManager.addDews(
                amount = task.rewardAmount,
                reason = "Task: ${task.title}",
                refId = "dew:task:$userId:$today:${task.taskKey}"
            )
            rewardEventBus.emit(RewardEvent.DewDropped(task.rewardAmount))
        } else if (task.tokenReward > 0) {
            economyManager.addTokens(
                amount = task.tokenReward,
                reason = "Completed: ${task.title}",
                refId = "task:$userId:$today:${task.taskKey}"
            )
            rewardEventBus.emit(RewardEvent.TokenDropped(task.tokenReward))
        }
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
            val pendingEntry = vocabularyPracticeRepository.getPendingReviewEntry()
            _vocabularyEntryState.value = VocabularyEntryUiState(
                pendingReviewCount = pendingEntry?.pendingWordCount ?: 0,
                pendingRoundId = pendingEntry?.roundId,
                shouldShowReviewBadge = (pendingEntry?.pendingWordCount ?: 0) > 0
            )
        }
    }
}
