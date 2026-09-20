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

data class TextbookAudioMaterial(
    val title: String,
    val titleZh: String?,
    val promptText: String?,
    val transcript: String?,
    val transcriptZh: String?,
    val audioUrl: String?
)

data class TextbookAudioUiState(
    val isLoading: Boolean = true,
    val material: TextbookAudioMaterial? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ListeningTextbookAudioViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listeningRemote: ListeningRemoteDataSource
) : ViewModel() {
    private val materialId =
        savedStateHandle.get<String>(Screen.ListeningTextbookAudio.ARG_MATERIAL_ID).orEmpty()

    private val _uiState = MutableStateFlow(TextbookAudioUiState())
    val uiState: StateFlow<TextbookAudioUiState> = _uiState.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _uiState.value = TextbookAudioUiState(isLoading = true)
            runCatching { listeningRemote.fetchMaterial(materialId) }
                .onSuccess { material ->
                    _uiState.value = if (material == null) {
                        TextbookAudioUiState(
                            isLoading = false,
                            errorMessage = "未找到这条教材音频。"
                        )
                    } else {
                        TextbookAudioUiState(
                            isLoading = false,
                            material = TextbookAudioMaterial(
                                title = material.title?.takeIf { it.isNotBlank() }
                                    ?: material.material_id,
                                titleZh = material.title_zh,
                                promptText = material.prompt_text,
                                transcript = material.transcript,
                                transcriptZh = material.transcript_zh,
                                audioUrl = material.audio_url
                            )
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value = TextbookAudioUiState(
                        isLoading = false,
                        errorMessage = error.message ?: "教材音频加载失败。"
                    )
                }
        }
    }
}
