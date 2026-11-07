package com.example.seedie.domain.model

@kotlinx.serialization.Serializable
data class VideoLesson(
    val id: String,
    val agencyId: String,
    val title: String,
    val description: String?,
    val storagePath: String,
    val durationSeconds: Int?
)
