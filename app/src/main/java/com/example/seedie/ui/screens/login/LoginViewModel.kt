package com.example.seedie.ui.screens.login

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
class LoginViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    //pattern format checker
    private val emailPattern = Regex(".*@.*")

    // UI 状态
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun onEmailChange(newEmail: String) { _email.value = newEmail }
    fun onPasswordChange(newPassword: String) { _password.value = newPassword }

    fun isEmailFormatValid(value: String = _email.value): Boolean = emailPattern.matches(value)

    // 核心登录方法，接收一个成功后的回调（跳转接口）
    fun login(onLoginSuccess: () -> Unit) {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "请输入邮箱和密码"
            return
        }

        if (!isEmailFormatValid()) {
            _errorMessage.value = "邮箱格式不正确"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = authService.login(_email.value, _password.value)
            if (result.isSuccess) {
                // 登录成功，触发外部传入的跳转接口
                onLoginSuccess()
            } else {
                _errorMessage.value = _errorMessage.value ?: "登录失败，请检查账号或密码"
            }
            _isLoading.value = false
        }
    }
}
