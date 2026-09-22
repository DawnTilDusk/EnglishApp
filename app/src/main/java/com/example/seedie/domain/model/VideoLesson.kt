package com.example.seedie.domain.model

data class VideoLesson(
    val id: String,
    val agencyId: String,
    val title: String,
    val description: String?,
    val storagePath: String,
    val durationSeconds: Int?
)
