package com.example.seedie.domain.model

sealed class RewardEvent {
    data class TokensDropped(val amount: Int) : RewardEvent()
    data class PlantLeveledUp(val plotIndex: Int, val newLevel: Int) : RewardEvent()
    data class AchievementUnlocked(val title: String) : RewardEvent()
}