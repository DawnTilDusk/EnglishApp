package com.example.seedie.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.local.dao.CheckInDao
import com.example.seedie.data.local.entity.CheckInEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.sync.SyncManager
import com.example.seedie.data.sync.SyncScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkInDao: CheckInDao,
    private val authService: AuthService,
    private val syncManager: SyncManager
) : ViewModel() {

    fun checkIn(onDone: () -> Unit) {
        viewModelScope.launch {
            // 等待 session 就绪（解决 restoreSessionFromAuth 异步竞争问题）
            val userId = authService.currentSession
                .filterNotNull()
                .first()
                .userId
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
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
            onDone()
        }
    }
}
