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
                        // 本地有有效的 Token（已登录）
                        // 尝试拉取业务层 Session（角色、昵称等）
                        authService.restoreSessionFromAuth()
                        _isLoggedIn.value = true
                    }
                    is SessionStatus.NotAuthenticated -> {
                        // 未登录，或用户主动登出
                        _isLoggedIn.value = false
                    }
                    is SessionStatus.NetworkError -> {
                        // 网络异常状态，为了不让用户卡在开屏页，赋值 false 退回登录界面
                        _isLoggedIn.value = false
                    }
                }
            }
        }
    }
}
