package com.example.seedie.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.community.CommunityFeedRefreshBus
import com.example.seedie.domain.model.CommunityPost
import com.example.seedie.domain.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CommunityFeedViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val refreshBus: CommunityFeedRefreshBus
) : ViewModel() {

    private val _posts = MutableStateFlow<List<CommunityPost>>(emptyList())
    val posts: StateFlow<List<CommunityPost>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _composerVisible = MutableStateFlow(false)
    val composerVisible: StateFlow<Boolean> = _composerVisible.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            refreshBus.ticks.collect { refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _posts.value = communityRepository.fetchFeed()
            } catch (e: Exception) {
                _message.value = e.message ?: "加载失败"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openComposer() {
        _composerVisible.value = true
    }

    fun dismissComposer() {
        _composerVisible.value = false
    }

    fun clearMessage() {
        _message.value = null
    }

    fun createPost(title: String?, body: String) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) {
            _message.value = "请填写正文"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = communityRepository.createPost(
                title = title?.trim()?.takeIf { it.isNotEmpty() },
                body = trimmed
            )
            _isSubmitting.value = false
            result.fold(
                onSuccess = {
                    _composerVisible.value = false
                    refresh()
                },
                onFailure = { e ->
                    _message.value = e.message ?: "发帖失败"
                }
            )
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = communityRepository.deletePost(postId)
            result.fold(
                onSuccess = { refresh() },
                onFailure = { e ->
                    _message.value = e.message ?: "删除失败"
                }
            )
        }
    }
}
