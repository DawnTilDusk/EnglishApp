package com.example.seedie.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.repository.DailyTaskManager
import com.example.seedie.domain.model.StudyDay
import com.example.seedie.domain.repository.PracticeAssignmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val database: SeedieDatabase,
    private val auth: AuthService,
    private val tasks: DailyTaskManager,
    private val assignments: PracticeAssignmentRepository,
    private val diagnostics: com.example.seedie.data.repository.LearningDiagnostics,
    private val activities: com.example.seedie.domain.repository.ActivityTrackingRepository
) : ViewModel() {
    private val dates = flow {
        while (true) { emit(StudyDay.date()); delay(1_000) }
    }.distinctUntilChanged()
    private val identity = combine(auth.currentSession, dates) { session, date -> session?.userId to date }
        .distinctUntilChanged().shareIn(viewModelScope, SharingStarted.Eagerly, replay = 1)
    val activityDays = identity.flatMapLatest { (userId, _) ->
        if (userId == null) flowOf(emptyList()) else activities.observeRecentDailyTotals(35)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _writingMessage = MutableStateFlow("正在检查写作作业…")
    val writingMessage = _writingMessage.asStateFlow()
    private val _writingAvailable = MutableStateFlow(false)
    val dailyTasks = identity.flatMapLatest { (userId, date) ->
        if (userId == null) flowOf(emptyList())
        else combine(database.dailyTaskDao().getTasksByUserAndDate(userId, date), _writingAvailable) { rows, available ->
            rows.filter { it.taskKey != "daily_writing" || it.isCompleted || available }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    init {
        viewModelScope.launch {
            identity.collectLatest { (userId, date) ->
                _writingAvailable.value = false
                if (userId == null) return@collectLatest
                tasks.ensureTasks(userId, date)
                launch { checkWriting(userId, date) }
                database.learningStateDao().observeWordCount(userId, date).collect {
                    tasks.reconcileVocabulary(userId, date)
                }
            }
        }
    }
    fun refreshWriting() {
        val userId = auth.currentSession.value?.userId ?: return
        viewModelScope.launch { checkWriting(userId, StudyDay.date()) }
    }
    fun recordTaskOpen(key: String) = diagnostics.record("task_opened", "daily_tasks", key)
    private suspend fun checkWriting(userId: String, date: String) {
        _writingMessage.value = "正在检查写作作业…"
        try {
            val available = assignments.listForModule("writing").any {
                it.status in setOf("pending", "in_progress") && !it.isOverdue
            }
            if (auth.currentSession.value?.userId != userId || StudyDay.date() != date) return
            _writingAvailable.value = available
            if (available) tasks.ensureWritingTask(userId, date)
            _writingMessage.value = if (available) "" else "暂无可提交的写作作业"
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            if (auth.currentSession.value?.userId == userId) _writingMessage.value = "写作作业检查失败，点击重试"
        }
    }
}
