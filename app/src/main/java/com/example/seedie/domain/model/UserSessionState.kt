package com.example.seedie.domain.model

data class UserSessionState(
    val tokens: Int = 0,
    val totalStudyTimeMinutes: Int = 0,
    val vocabularySize: Int = 0
)