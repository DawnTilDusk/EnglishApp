package com.example.seedie.ui.screens.learning.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.model.PracticeCatalogItem
import com.example.seedie.domain.repository.PracticeCatalogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PracticeCatalogUiState(
    val moduleId: String = "reading",
    val title: String = "阅读题库",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val items: List<PracticeCatalogItem> = emptyList()
)

@HiltViewModel
class PracticeCatalogViewModel @Inject constructor(
    private val catalogRepository: PracticeCatalogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PracticeCatalogUiState())
    val uiState = _uiState.asStateFlow()

    private var loadedModuleId: String? = null

    fun initialize(moduleId: String) {
        if (loadedModuleId == moduleId &&
            _uiState.value.items.isNotEmpty() &&
            _uiState.value.errorMessage == null
        ) {
            return
        }
        loadedModuleId = moduleId
        _uiState.update {
            it.copy(
                moduleId = moduleId,
                title = if (moduleId == "listening") "听力题库" else "阅读题库",
                isLoading = true,
                errorMessage = null
            )
        }
        refresh()
    }

    fun refresh() {
        val moduleId = loadedModuleId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                catalogRepository.listCatalog(moduleId)
            }.onSuccess { items ->
                _uiState.update {
                    it.copy(isLoading = false, items = items, errorMessage = null)
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "题库加载失败"
                    )
                }
            }
        }
    }
}
