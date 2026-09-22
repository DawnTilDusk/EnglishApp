package com.example.seedie.ui.screens.learning.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.VideoLessonRemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VideoLessonPlayerUiState(
    val loading: Boolean = true,
    val signedUrl: String? = null,
    val error: String? = null
)

@HiltViewModel
class VideoLessonPlayerViewModel @Inject constructor(
    private val remote: VideoLessonRemoteDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoLessonPlayerUiState())
    val uiState: StateFlow<VideoLessonPlayerUiState> = _uiState.asStateFlow()

    private var storagePath: String? = null

    fun prepare(path: String) {
        if (storagePath == path && _uiState.value.signedUrl != null) return
        storagePath = path
        refresh()
    }

    fun refresh() {
        val path = storagePath ?: return
        viewModelScope.launch {
            _uiState.value = VideoLessonPlayerUiState(loading = true)
            try {
                val url = remote.createPlaybackUrl(path)
                _uiState.value = VideoLessonPlayerUiState(loading = false, signedUrl = url)
            } catch (e: Exception) {
                _uiState.value = VideoLessonPlayerUiState(
                    loading = false,
                    error = mapVideoLoadError(e)
                )
            }
        }
    }
}
