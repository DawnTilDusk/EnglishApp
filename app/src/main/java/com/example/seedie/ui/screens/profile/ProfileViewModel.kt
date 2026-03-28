package com.example.seedie.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.UserSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    economyManager: EconomyManager,
    userSessionRepository: UserSessionRepository
) : ViewModel() {

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
}