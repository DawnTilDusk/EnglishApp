package com.example.seedie.ui.screens.teacher.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.StudentStats
import com.example.seedie.domain.model.StudentSummary
import com.example.seedie.domain.repository.TeacherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _students = MutableStateFlow<List<StudentSummary>>(emptyList())
    val students: StateFlow<List<StudentSummary>> = _students.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _students.value = teacherRepository.fetchMyStudents()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "加载学生列表失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@HiltViewModel
class StudentDetailViewModel @Inject constructor(
    private val teacherRepository: TeacherRepository
) : ViewModel() {

    private val _stats = MutableStateFlow<StudentStats?>(null)
    val stats: StateFlow<StudentStats?> = _stats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun load(studentId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            teacherRepository.getStudentStats(studentId)
                .onSuccess { _stats.value = it }
                .onFailure { _errorMessage.value = it.message ?: "加载失败" }
            _isLoading.value = false
        }
    }
}
