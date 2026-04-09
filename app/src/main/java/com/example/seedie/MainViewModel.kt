package com.example.seedie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.AuthService
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

    // null 代表还在检查中，true 代表已登录，false 代表未登录
    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn: StateFlow<Boolean?> = _isLoggedIn.asStateFlow()

    private var businessSessionRestored = false

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            authService.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Initializing -> {
                        // 正在加载本地 Token，保持 null（显示 Splash Screen）
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
                        // 未登录，或用户主动登出
                        _isLoggedIn.value = false
                        businessSessionRestored = false
                    }
                    is SessionStatus.RefreshFailure -> {
                        // Token 刷新失败（可能已过期或被撤销），退回登录界面重新登录
                        _isLoggedIn.value = false
                        businessSessionRestored = false
                    }
                }
            }
        }
    }
}
