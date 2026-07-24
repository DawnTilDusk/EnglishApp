package com.example.seedie.domain.model

/**
 * In-memory session study stats.
 * Token balance is not tracked here — use [com.example.seedie.domain.repository.EconomyManager].
 */
data class UserSessionState(
    @Deprecated("Tokens are owned by EconomyManager; this field stays at 0")
    val tokens: Int = 0,
    val totalStudyTimeMinutes: Int = 0,
    val vocabularySize: Int = 0
)
