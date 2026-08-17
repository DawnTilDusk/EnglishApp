package com.example.seedie.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.data.local.entity.DailyTaskEntity
import com.example.seedie.data.remote.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskDao: DailyTaskDao,
    private val authService: AuthService
) : ViewModel() {

    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyTasks: StateFlow<List<DailyTaskEntity>> = authService.currentSession
        .flatMapLatest { session ->
            val userId = session?.userId
            if (userId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                taskDao.getTasksByUserAndDate(userId, todayDate)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            authService.currentSession.collect { session ->
                val userId = session?.userId ?: return@collect
                ensureDefaultTasks(userId)
            }
        }
    }

    private suspend fun ensureDefaultTasks(userId: String) {
        val tasks = taskDao.getTasksByUserAndDate(userId, todayDate).first()
        if (tasks.isNotEmpty()) return
        val dewDefaults = listOf(
            Triple("daily_vocabulary", "学习单词 20 个", 8),
            Triple("daily_listening", "完成一次听力训练", 8),
            Triple("daily_reading", "完成一次阅读训练", 8)
        )
        dewDefaults.forEach { (key, title, amount) ->
            taskDao.insertTask(
                DailyTaskEntity(
                    userId = userId,
                    date = todayDate,
                    title = title,
                    rewardType = "dew",
                    rewardAmount = amount,
                    tokenReward = 0,
                    autoClaim = true,
                    taskKey = key
                )
            )
        }
        val tokenChallenges = listOf(
            Triple("daily_writing", "提交一次作文", 10)
        )
        tokenChallenges.forEach { (key, title, tokens) ->
            taskDao.insertTask(
                DailyTaskEntity(
                    userId = userId,
                    date = todayDate,
                    title = title,
                    rewardType = "token",
                    rewardAmount = 0,
                    tokenReward = tokens,
                    autoClaim = true,
                    taskKey = key
                )
            )
        }
    }

}
