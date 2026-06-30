package com.example.seedie.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.AuthSession
import com.example.seedie.domain.profile.ProfileGradeOptions
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.ProfileRepository
import com.example.seedie.domain.repository.UserProfile
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
    val grade: String = ProfileGradeOptions.DEFAULT_GRADE,
    val phone: String = "未绑定手机号",
    val email: String = "未绑定邮箱",
    val avatarToneIndex: Int = 0
) {
    val avatarMonogram: String
        get() = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "S"
}

data class ProfileEditorUiState(
    val profile: ProfileIdentityUiState = ProfileIdentityUiState(),
    val draft: ProfileIdentityUiState = ProfileIdentityUiState(),
    val isEditOverlayVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isPhoneDialogVisible: Boolean = false,
    val phoneDraft: String = "",
    val phoneInputError: String? = null,
    val isBindingPhone: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    economyManager: EconomyManager,
    userSessionRepository: UserSessionRepository,
    private val authService: AuthService,
    private val profileRepository: ProfileRepository
) : ViewModel() {
    private val phonePattern = Regex("^\\+?[0-9]{11,13}$")

    private val _profileEditorUiState = MutableStateFlow(
        ProfileEditorUiState(
            profile = buildFallbackProfile(authService.currentSession.value),
            draft = buildFallbackProfile(authService.currentSession.value),
            isLoading = true
        )
    )
    val profileEditorUiState: StateFlow<ProfileEditorUiState> = _profileEditorUiState.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

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

    init {
        refreshProfile()
    }

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
            draft = _profileEditorUiState.value.draft.copy(
                grade = ProfileGradeOptions.normalize(value)
            )
        )
    }

    fun saveProfileEdits() {
        val draft = _profileEditorUiState.value.draft
        val normalizedGrade = ProfileGradeOptions.normalize(draft.grade)
        if (draft.displayName.isBlank()) {
            _message.value = "用户名不能为空"
            return
        }

        _profileEditorUiState.value = _profileEditorUiState.value.copy(isSaving = true)
        viewModelScope.launch {
            profileRepository.updateMyProfile(
                displayName = draft.displayName,
                grade = normalizedGrade,
                avatarToneIndex = draft.avatarToneIndex
            )
                .onSuccess { profile ->
                    val mappedProfile = profile.toUiState(authService.currentSession.value)
                    _profileEditorUiState.value = _profileEditorUiState.value.copy(
                        profile = mappedProfile,
                        draft = mappedProfile,
                        isEditOverlayVisible = false,
                        isSaving = false
                    )
                    _message.value = "资料已更新"
                }
                .onFailure { error ->
                    _profileEditorUiState.value = _profileEditorUiState.value.copy(isSaving = false)
                    _message.value = error.message ?: "资料保存失败"
                }
        }
    }

    fun openPhoneBindingDialog() {
        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            isPhoneDialogVisible = true,
            phoneDraft = extractEditablePhone(_profileEditorUiState.value.profile.phone),
            phoneInputError = null
        )
    }

    fun dismissPhoneBindingDialog() {
        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            isPhoneDialogVisible = false,
            phoneDraft = "",
            phoneInputError = null,
            isBindingPhone = false
        )
    }

    fun updatePhoneDraft(value: String) {
        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            phoneDraft = value,
            phoneInputError = null
        )
    }

    fun bindPhone() {
        val normalizedPhone = normalizePhone(_profileEditorUiState.value.phoneDraft)
        if (!phonePattern.matches(normalizedPhone)) {
            _profileEditorUiState.value = _profileEditorUiState.value.copy(
                phoneInputError = "手机号格式不正确"
            )
            return
        }

        _profileEditorUiState.value = _profileEditorUiState.value.copy(
            isBindingPhone = true,
            phoneInputError = null
        )
        viewModelScope.launch {
            profileRepository.bindMyPhone(normalizedPhone)
                .onSuccess { profile ->
                    val mappedProfile = profile.toUiState(authService.currentSession.value)
                    _profileEditorUiState.value = _profileEditorUiState.value.copy(
                        profile = mappedProfile,
                        draft = mappedProfile,
                        isPhoneDialogVisible = false,
                        phoneDraft = "",
                        phoneInputError = null,
                        isBindingPhone = false
                    )
                    _message.value = "手机号已绑定"
                }
                .onFailure { error ->
                    _profileEditorUiState.value = _profileEditorUiState.value.copy(
                        isBindingPhone = false,
                        phoneInputError = error.message ?: "手机号绑定失败"
                    )
                }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refreshProfile() {
        _profileEditorUiState.value = _profileEditorUiState.value.copy(isLoading = true)
        viewModelScope.launch {
            runCatching { profileRepository.getMyProfile() }
                .onSuccess { profile ->
                    val mappedProfile = profile.toUiState(authService.currentSession.value)
                    _profileEditorUiState.value = _profileEditorUiState.value.copy(
                        profile = mappedProfile,
                        draft = mappedProfile,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    val fallbackProfile = buildFallbackProfile(authService.currentSession.value)
                    _profileEditorUiState.value = _profileEditorUiState.value.copy(
                        profile = fallbackProfile,
                        draft = fallbackProfile,
                        isLoading = false
                    )
                    _message.value = error.message ?: "资料加载失败"
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.logout()
        }
    }

    private fun buildFallbackProfile(session: AuthSession?): ProfileIdentityUiState {
        val displayName = session?.displayName
            ?.takeIf { it.isNotBlank() }
            ?: session?.studentName?.takeIf { it.isNotBlank() }
            ?: "Seedie 学员"
        return ProfileIdentityUiState(
            displayName = displayName,
            grade = ProfileGradeOptions.DEFAULT_GRADE,
            phone = "未绑定手机号",
            email = "未绑定邮箱"
        )
    }

    private fun normalizePhone(input: String): String {
        return input.trim().replace(" ", "").replace("-", "")
    }

    private fun extractEditablePhone(displayValue: String): String {
        val normalized = normalizePhone(displayValue)
        return normalized.takeIf { phonePattern.matches(it) }.orEmpty()
    }
}

private fun UserProfile.toUiState(session: AuthSession?): ProfileIdentityUiState {
    val resolvedDisplayName = displayName
        ?.takeIf { it.isNotBlank() }
        ?: session?.displayName?.takeIf { it.isNotBlank() }
        ?: session?.studentName?.takeIf { it.isNotBlank() }
        ?: "Seedie 学员"

    return ProfileIdentityUiState(
        displayName = resolvedDisplayName,
        grade = ProfileGradeOptions.normalize(grade),
        phone = phone?.takeIf { it.isNotBlank() } ?: "未绑定手机号",
        email = email?.takeIf { it.isNotBlank() } ?: "未绑定邮箱",
        avatarToneIndex = avatarToneIndex.coerceIn(0, 2)
    )
}
