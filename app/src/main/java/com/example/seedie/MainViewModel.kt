package com.example.seedie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    // null 代表还在检查中，true 代表已登录，false 代表未登录
    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = null
            authService.restoreSessionFromAuth()
            _isLoggedIn.value = authService.currentSession.value != null
        }

        viewModelScope.launch {
            authService.currentSession.collect { session ->
                if (_isLoggedIn.value != null) {
                    _isLoggedIn.value = session != null
                }
            }
        }
    }
}
