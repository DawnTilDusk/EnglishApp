package com.example.seedie.domain.model

sealed class RewardEvent {
    data class TokenDropped(val amount: Int) : RewardEvent()
    data class DewDropped(val amount: Int) : RewardEvent()
    data class PlantLeveledUp(val plotIndex: Int, val newLevel: Int) : RewardEvent()
    data class PlantGrown(val speciesId: String, val status: String) : RewardEvent()
    data class AchievementUnlocked(val badgeId: String) : RewardEvent()
}
