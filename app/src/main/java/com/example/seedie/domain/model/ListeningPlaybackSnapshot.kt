package com.example.seedie.domain.model

@kotlinx.serialization.Serializable
data class ListeningPlaybackSnapshot(
    val url: String?,
    val text: String,
    val positionMs: Long,
    val speechCursor: Int,
    val usingSpeech: Boolean
)
