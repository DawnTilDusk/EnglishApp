package com.example.seedie.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.data.local.entity.DailyTaskEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.usecase.RewardEventBus
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
    private val authService: AuthService,
    private val economyManager: EconomyManager,
    private val rewardEventBus: RewardEventBus
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

        taskDao.insertTask(
            DailyTaskEntity(
                userId = userId,
                date = todayDate,
                title = "背诵 20 个单词",
                rewardAmount = 10
            )
        )
        taskDao.insertTask(
            DailyTaskEntity(
                userId = userId,
                date = todayDate,
                title = "完成一次语法测验",
                rewardAmount = 15
            )
        )
        taskDao.insertTask(
            DailyTaskEntity(
                userId = userId,
                date = todayDate,
                title = "听力训练 10 分钟",
                rewardAmount = 20
            )
        )
    }

    fun onTaskClicked(task: DailyTaskEntity) {
        if (task.isCompleted) return

        viewModelScope.launch {
            val userId = authService.currentSession.value?.userId ?: return@launch
            if (task.userId != userId) return@launch

            taskDao.updateTask(task.copy(isCompleted = true))

            val taskKey = normalizeTaskKey(task.title)
            economyManager.addTokens(
                amount = task.rewardAmount,
                reason = "Completed: ${task.title}",
                refId = "task:$userId:$todayDate:$taskKey"
            )

            rewardEventBus.emit(RewardEvent.TokenDropped(task.rewardAmount))
        }
    }

    private fun normalizeTaskKey(title: String): String {
        return title.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), "_")
    }
}
