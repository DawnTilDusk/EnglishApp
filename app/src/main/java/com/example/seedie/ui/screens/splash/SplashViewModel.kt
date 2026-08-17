package com.example.seedie.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.dao.CheckInDao
import com.example.seedie.data.local.entity.CheckInEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.sync.SyncManager
import com.example.seedie.data.sync.SyncScope
import com.example.seedie.domain.model.DewConstants
import com.example.seedie.domain.model.RewardEvent
import com.example.seedie.domain.repository.DewManager
import com.example.seedie.domain.usecase.RewardEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SplashUiState(
    val isLoading: Boolean = true,
    val shouldSkipCheckIn: Boolean = false
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkInDao: CheckInDao,
    private val authService: AuthService,
    private val syncManager: SyncManager,
    private val dewManager: DewManager,
    private val rewardEventBus: RewardEventBus
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        loadCheckInStatus()
    }

    private fun loadCheckInStatus() {
        viewModelScope.launch {
            val userId = awaitUserId()
            val existing = checkInDao.getCheckInByDate(userId, today())
            _uiState.value = SplashUiState(
                isLoading = false,
                shouldSkipCheckIn = existing?.isCheckedIn == true
            )
        }
    }

    fun checkIn() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userId = awaitUserId()
            val today = today()
            val existing = checkInDao.getCheckInByDate(userId, today)
            if (existing == null || !existing.isCheckedIn) {
                checkInDao.insertOrUpdateCheckIn(
                    CheckInEntity(
                        userId = userId,
                        date = today,
                        isCheckedIn = true,
                        studyTimeMinutes = 0
                    )
                )
                val streakDay = computeStreakDay(userId, today)
                val dewAmount = DewConstants.dewForStreakDay(streakDay)
                val grantedDews = dewManager.addDews(
                    amount = dewAmount,
                    reason = "Daily Check-In",
                    refId = "dew:checkin:$userId:$today"
                )
                if (grantedDews > 0) {
                    rewardEventBus.emit(RewardEvent.DewDropped(grantedDews))
                }
                syncManager.syncNow(SyncScope.CHECK_IN)
            }
            _uiState.value = SplashUiState(
                isLoading = false,
                shouldSkipCheckIn = true
            )
        }
    }

    private suspend fun computeStreakDay(userId: String, todayStr: String): Int {
        val today = dateFormat.parse(todayStr) ?: return 1
        val recent = checkInDao.getRecentCheckIns(userId, todayStr)
            .filter { it.isCheckedIn }
            .associateBy { it.date }
        val cal = Calendar.getInstance()
        cal.time = today
        var streak = 0
        val checkFormat = dateFormat
        for (i in 0..30) {
            val key = checkFormat.format(cal.time)
            if (recent.containsKey(key)) {
                streak += 1
                cal.add(Calendar.DAY_OF_MONTH, -1)
            } else if (i == 0) {
                cal.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                break
            }
        }
        return streak.coerceAtLeast(1)
    }

    private suspend fun awaitUserId(): String {
        return authService.currentSession
            .filterNotNull()
            .first()
            .userId
    }

    private fun today(): String {
        return dateFormat.format(Date())
    }
}
