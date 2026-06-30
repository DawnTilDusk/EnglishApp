package com.example.seedie.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.AuthSession
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.UserSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileIdentityUiState(
    val displayName: String = "Seedie 学员",
    val grade: String = "一年级",
    val phone: String = "138****1234",
    val email: String = "seedie.student@example.com",
    val avatarToneIndex: Int = 0
) {
    val avatarMonogram: String
        get() = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
}

data class ProfileEditorUiState(
    val profile: ProfileIdentityUiState = ProfileIdentityUiState(),
    val draft: ProfileIdentityUiState = ProfileIdentityUiState(),
    val isEditOverlayVisible: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    economyManager: EconomyManager,
    userSessionRepository: UserSessionRepository,
    private val authService: AuthService
) : ViewModel() {
    private val _profileEditorUiState = MutableStateFlow(
        ProfileEditorUiState(
            profile = buildInitialProfile(authService.currentSession.value),
            draft = buildInitialProfile(authService.currentSession.value)
        )
    )
    val profileEditorUiState: StateFlow<ProfileEditorUiState> = _profileEditorUiState.asStateFlow()

    val totalTokens: StateFlow<Int> = economyManager.totalTokens
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val badges: StateFlow<List<BadgeConfig>> = userSessionRepository.sessionState
        .combine(economyManager.totalTokens) { session, tokens ->
            listOf(
                BadgeConfig(
                    id = "first_blood",
                    name = "初见",
                    description = "第一次打卡",
                    isUnlocked = true // Mocked for MVP
                ),
                BadgeConfig(
                    id = "vocab_100",
                    name = "百词斩",
                    description = "词汇量达到100",
                    isUnlocked = session.vocabularySize >= 100
                ),
                BadgeConfig(
                    id = "rich_kid",
                    name = "小富翁",
                    description = "累计获得50代币",
                    isUnlocked = tokens >= 50
                ),
                BadgeConfig(
                    id = "secret_1",
                    name = "???",
                    description = "坚持学习30天解锁",
                    isUnlocked = false
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun openProfileEditor() {
        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            draft = _profileEditorUiState.value.profile,
            isEditOverlayVisible = true
        )
    }

    fun dismissProfileEditor() {
        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            draft = _profileEditorUiState.value.profile,
            isEditOverlayVisible = false
        )
    }

    fun cycleDraftAvatarTone() {
        val nextToneIndex = (_profileEditorUiState.value.draft.avatarToneIndex + 1) % 3
        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            draft = _profileEditorUiState.value.draft.copy(avatarToneIndex = nextToneIndex)
        )
    }

    fun updateDraftDisplayName(value: String) {
        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            draft = _profileEditorUiState.value.draft.copy(displayName = value)
        )
    }

    fun updateDraftGrade(value: String) {
        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            draft = _profileEditorUiState.value.draft.copy(grade = value)
        )
    }

    fun saveProfileEdits() {
        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            profile = _profileEditorUiState.value.draft,
            isEditOverlayVisible = false
        )
    }

    fun logout() {
        viewModelScope.launch {
            authService.logout()
        }
    }

    private fun buildInitialProfile(session: AuthSession?): ProfileIdentityUiState {
        val displayName = session?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: session?.studentName?.takeIf { it.isNotBlank() }
            ?: "Seedie 学员"
        return ProfileIdentityUiState(displayName = displayName)
    }
}
