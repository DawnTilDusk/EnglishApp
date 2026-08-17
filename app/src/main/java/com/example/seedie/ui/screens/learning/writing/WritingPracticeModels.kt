package com.example.seedie.ui.screens.learning.writing

import com.example.seedie.domain.model.WritingPrompt

data class WritingPracticeUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val title: String = "",
    val status: String = "pending",
    val prompt: WritingPrompt? = null,
    val previewUri: String? = null,
    val selectedFileName: String? = null,
    val originalSignedUrl: String? = null,
    val annotatedSignedUrl: String? = null,
    val score: Int? = null,
    val maxScore: Int? = null,
    val feedbackText: String? = null,
    val earnedTokens: Int = 0,
    val earnedDews: Int = 0,
    val isPdfOriginal: Boolean = false,
    val isPdfAnnotated: Boolean = false
)
