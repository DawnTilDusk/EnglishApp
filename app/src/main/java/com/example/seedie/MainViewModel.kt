package com.example.seedie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    val currentSession: StateFlow<AuthSession?> = authService.currentSession

    private var businessSessionRestored = false

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            authService.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> {
                        _isLoggedIn.value = null
                    }
                    is SessionStatus.Authenticated -> {
                        _isLoggedIn.value = true
                        if (!businessSessionRestored) {
                            businessSessionRestored = true
                            viewModelScope.launch {
                                authService.restoreSessionFromAuth()
                            }
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _isLoggedIn.value = false
                        businessSessionRestored = false
                    }
                    is SessionStatus.RefreshFailure -> {
                        _isLoggedIn.value = false
                        businessSessionRestored = false
                    }
                }
            }
        }
    }
}
