package com.example.seedie.ui.screens.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherMainViewModel @Inject constructor(
    private val authService: AuthService
) : ViewModel() {

    fun logout() {
        viewModelScope.launch {
            authService.logout()
        }
    }
}
