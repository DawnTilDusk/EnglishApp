package com.example.seedie.ui.screens.learning.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.VideoLessonRemoteDataSource
import com.example.seedie.domain.model.VideoLesson
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VideoLessonListUiState(
    val loading: Boolean = true,
    val lessons: List<VideoLesson> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class VideoLessonListViewModel @Inject constructor(
    private val authService: AuthService,
    private val remote: VideoLessonRemoteDataSource
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoLessonListUiState())
    val uiState: StateFlow<VideoLessonListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true, error = null)
            val session = authService.refreshBusinessSession().getOrElse { error ->
                _uiState.value = VideoLessonListUiState(
                    loading = false,
                    error = error.message ?: "无法加载视频课"
                )
                return@launch
            }
            if (session?.agencyId == null) {
                _uiState.value = VideoLessonListUiState(
                    loading = false,
                    error = "还没有绑定机构，暂时看不到视频课"
                )
                return@launch
            }
            try {
                val lessons = remote.fetchPublishedLessons()
                _uiState.value = VideoLessonListUiState(loading = false, lessons = lessons)
            } catch (e: Exception) {
                _uiState.value = VideoLessonListUiState(
                    loading = false,
                    error = mapVideoLoadError(e)
                )
            }
        }
    }
}
