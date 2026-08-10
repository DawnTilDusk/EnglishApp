package com.example.seedie.ui.screens.learning.listening.textbook

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.ListeningRemoteDataSource
import com.example.seedie.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TextbookSection(
    val materialId: String,
    val title: String,
    val subtitle: String?
)

data class TextbookUnit(
    val unitRef: String,
    val title: String,
    val sections: List<TextbookSection>
)

data class TextbookUnitsUiState(
    val units: List<TextbookUnit> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ListeningTextbookUnitsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listeningRemote: ListeningRemoteDataSource
) : ViewModel() {

    private val bookId: String =
        savedStateHandle.get<String>(Screen.ListeningTextbookUnits.ARG_BOOK_ID).orEmpty()

    private val _uiState = MutableStateFlow(TextbookUnitsUiState())
    val uiState: StateFlow<TextbookUnitsUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val result = runCatching { listeningRemote.fetchMaterialsByBook(bookId) }
            _uiState.value = result.fold(
                onSuccess = { materials ->
                    val units = materials
                        .groupBy { it.unit_ref ?: "其他" }
                        .toList()
                        .map { (unitRef, items) ->
                            TextbookUnit(
                                unitRef = unitRef,
                                title = unitRef,
                                sections = items.map { material ->
                                    TextbookSection(
                                        materialId = material.material_id,
                                        title = material.section_ref
                                            ?: material.title
                                            ?: material.material_id,
                                        subtitle = material.title_zh ?: material.title
                                    )
                                }
                            )
                        }
                    TextbookUnitsUiState(units = units, loading = false)
                },
                onFailure = { error ->
                    _uiState.value.copy(
                        loading = false,
                        error = error.message ?: "音频清单加载失败"
                    )
                }
            )
        }
    }
}
