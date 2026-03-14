package com.example.testenglishlearningmachine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.testenglishlearningmachine.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository(application)

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _wordsLearned = MutableStateFlow(0)
    val wordsLearned: StateFlow<Int> = _wordsLearned.asStateFlow()

    init {
        viewModelScope.launch {
            repository.userName.collectLatest { _userName.value = it }
        }
        viewModelScope.launch {
            repository.wordsLearned.collectLatest { _wordsLearned.value = it }
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            repository.saveUserName(name)
        }
    }
}
