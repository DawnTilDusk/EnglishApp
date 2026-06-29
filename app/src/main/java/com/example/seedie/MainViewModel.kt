package com.example.seedie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object LoggedOut : AuthUiState
    data class LoggedIn(val session: AuthSession) : AuthUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    val authUiState: StateFlow<AuthUiState> = combine(
        authService.sessionStatus,
        authService.currentSession,
        authService.isLoginInProgress
    ) { status, session, loginInProgress ->
        when (status) {
            is SessionStatus.Initializing -> AuthUiState.Loading
            is SessionStatus.Authenticated -> when {
                session != null -> AuthUiState.LoggedIn(session)
                loginInProgress -> AuthUiState.LoggedOut
                else -> AuthUiState.Loading
            }
            is SessionStatus.NotAuthenticated,
            is SessionStatus.RefreshFailure -> AuthUiState.LoggedOut
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AuthUiState.Loading
    )

    private var businessSessionRestored = false

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            authService.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> {
                        Unit
                    }
                    is SessionStatus.Authenticated -> {
                        if (
                            !businessSessionRestored &&
                            authService.currentSession.value == null &&
                            !authService.isLoginInProgress.value
                        ) {
                            businessSessionRestored = true
                            viewModelScope.launch {
                                val result = authService.restoreSessionFromAuth()
                                if (result.isFailure && authService.currentSession.value == null) {
                                    authService.setPendingLoginError(
                                        result.exceptionOrNull()?.message ?: "会话恢复失败，请重新登录"
                                    )
                                    authService.cleanupFailedLogin()
                                }
                            }
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        businessSessionRestored = false
                    }
                    is SessionStatus.RefreshFailure -> {
                        businessSessionRestored = false
                    }
                }
            }
        }
    }
}
