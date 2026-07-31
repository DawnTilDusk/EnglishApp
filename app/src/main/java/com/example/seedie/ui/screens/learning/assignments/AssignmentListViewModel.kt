package com.example.seedie.ui.screens.learning.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.repository.PracticeAssignmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AssignmentListViewModel @Inject constructor(
    private val repository: PracticeAssignmentRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AssignmentListUiState())
    val uiState = _uiState.asStateFlow()

    private var loadedModuleId: String? = null

    fun initialize(moduleId: String) {
        val title = when (moduleId) {
            "listening" -> "听力训练"
            else -> "阅读训练"
        }
        val moduleChanged = loadedModuleId != moduleId
        loadedModuleId = moduleId
        if (moduleChanged) {
            _uiState.value = AssignmentListUiState(
                moduleId = moduleId,
                moduleTitle = title,
                isLoading = true
            )
        } else {
            _uiState.update {
                it.copy(moduleId = moduleId, moduleTitle = title)
            }
        }
        // Always re-fetch: ViewModel survives practice navigation, stale status
        // would keep submitted work on the incomplete tab.
        refresh()
    }

    fun onTabSelected(tab: AssignmentListTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onRetry() {
        refresh()
    }

    fun refresh() {
        val moduleId = loadedModuleId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                repository.listForModule(moduleId)
            }.onSuccess { items ->
                val incomplete = items
                    .filter { it.status != "submitted" }
                    .sortedWith(
                        compareByDescending<com.example.seedie.domain.model.PracticeAssignmentListItem> { it.isOverdue }
                            .thenBy { it.dueAtEpochMs }
                    )
                val completed = items
                    .filter { it.status == "submitted" }
                    .sortedByDescending { it.submittedAtEpochMs ?: 0L }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        incomplete = incomplete,
                        completed = completed,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "加载作业失败"
                    )
                }
            }
        }
    }
}
