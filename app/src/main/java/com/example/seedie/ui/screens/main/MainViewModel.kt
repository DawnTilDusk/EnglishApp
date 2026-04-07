package com.example.seedie.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.UserSessionRepository
import com.example.seedie.domain.usecase.RewardEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val economyManager: EconomyManager,
    private val taskDao: DailyTaskDao,
    private val rewardEventBus: RewardEventBus
) : ViewModel() {

    private val handledSessions = mutableSetOf<String>()

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
                userSessionRepository.addTokens(result.earnedTokens)
                economyManager.addTokens(result.earnedTokens, "Vocabulary Practice")
                rewardEventBus.emit(RewardEvent.TokenDropped(result.earnedTokens))
            }
            if (result.isCompleted) {
                completeTodayVocabularyTask()
            }
        }
    }

    private suspend fun completeTodayVocabularyTask() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val task = taskDao.getTasksByDate(today)
            .first()
            .firstOrNull { !it.isCompleted && (it.title.contains("背诵") || it.title.contains("单词")) }
            ?: return

        taskDao.updateTask(task.copy(isCompleted = true))
    }
}
