package com.example.seedie.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.dao.CheckInDao
import com.example.seedie.data.local.entity.CheckInEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.sync.SyncManager
import com.example.seedie.data.sync.SyncScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SplashUiState(
    val isLoading: Boolean = true,
    val shouldSkipCheckIn: Boolean = false
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkInDao: CheckInDao,
    private val authService: AuthService,
    private val syncManager: SyncManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

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
                syncManager.syncNow(SyncScope.CHECK_IN)
            }
            _uiState.value = SplashUiState(
                isLoading = false,
                shouldSkipCheckIn = true
            )
        }
    }

    private suspend fun awaitUserId(): String {
        // 等待 session 就绪，避免 restoreSessionFromAuth 异步恢复时读取到空值。
        return authService.currentSession
            .filterNotNull()
            .first()
            .userId
    }

    private fun today(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
