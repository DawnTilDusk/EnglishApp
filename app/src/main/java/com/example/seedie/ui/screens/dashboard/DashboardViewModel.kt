package com.example.seedie.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.dao.CheckInDao
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.data.local.entity.DailyTaskEntity
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.usecase.RewardEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val taskDao: DailyTaskDao,
    private val checkInDao: CheckInDao,
    private val economyManager: EconomyManager,
    private val rewardEventBus: RewardEventBus
) : ViewModel() {

    private val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val dailyTasks: StateFlow<List<DailyTaskEntity>> = taskDao.getTasksByDate(todayDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initialize some mock tasks for MVP if empty
        viewModelScope.launch {
            taskDao.getTasksByDate(todayDate).collect { tasks ->
                if (tasks.isEmpty()) {
                    taskDao.insertTask(DailyTaskEntity(date = todayDate, title = "背诵 20 个单词", rewardAmount = 10))
                    taskDao.insertTask(DailyTaskEntity(date = todayDate, title = "完成一次语法测验", rewardAmount = 15))
                    taskDao.insertTask(DailyTaskEntity(date = todayDate, title = "听力训练 10 分钟", rewardAmount = 20))
                }
            }
        }
    }

    fun onTaskClicked(task: DailyTaskEntity) {
        if (task.isCompleted) return // Already completed

        viewModelScope.launch {
            // 1. Mark task as completed
            val updatedTask = task.copy(isCompleted = true)
            taskDao.updateTask(updatedTask)

            // 2. Add tokens to EconomyManager
            economyManager.addTokens(task.rewardAmount, "Completed: ${task.title}")

            // 3. Trigger Global Reward Event for animation
            rewardEventBus.emit(RewardEvent.TokenDropped(task.rewardAmount))
        }
    }
}