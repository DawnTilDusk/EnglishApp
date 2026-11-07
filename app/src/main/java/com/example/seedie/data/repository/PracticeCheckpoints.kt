package com.example.seedie.data.repository

import com.example.seedie.ui.screens.learning.reading.ReadingPracticeSession
import com.example.seedie.ui.screens.learning.reading.ReadingPracticeUiState
import com.example.seedie.ui.screens.learning.listening.ListeningPracticeSession
import com.example.seedie.ui.screens.learning.listening.ListeningPracticeUiState
import kotlinx.serialization.Serializable

@Serializable
data class ReadingCheckpoint(
    val session: ReadingPracticeSession,
    val state: ReadingPracticeUiState,
    val answers: Map<String, String>,
    val speciesId: String,
    val submissionPending: Boolean,
    val submitted: Boolean,
    val completedAt: Long
)

@Serializable
data class ListeningCheckpoint(
    val session: ListeningPracticeSession,
    val state: ListeningPracticeUiState,
    val answers: Map<String, String>,
    val drafts: Map<String, String>,
    val wrongIds: Set<String>,
    val speciesId: String,
    val submissionPending: Boolean,
    val submitted: Boolean,
    val completedAt: Long,
    val playback: com.example.seedie.domain.model.ListeningPlaybackSnapshot? = null
)
