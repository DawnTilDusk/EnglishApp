package com.example.seedie.ui.screens.learning.listening

import com.example.seedie.ui.screens.learning.practice.AnswerStatus
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption

enum class ListeningPracticeStage {
    Loading,
    Ready,
    AnswerEvaluated,
    Completed,
    Error
}

data class ListeningQuestionItem(
    val questionId: String,
    val questionType: String?,
    val stem: String,
    val rewardToken: Int,
    val options: List<VocabularyPracticeOption>,
    val correctOptionId: String,
    val explanation: String?
)

data class ListeningMaterialItem(
    val materialId: String,
    val title: String,
    val titleZh: String?,
    val materialType: String?,
    val promptText: String?,
    val transcript: String?,
    val audioUrl: String?,
    val estimatedSeconds: Int,
    val questions: List<ListeningQuestionItem>
)

data class ListeningPracticeSession(
    val sessionId: String,
    val materials: List<ListeningMaterialItem>
)

data class ListeningPracticeUiState(
    val stage: ListeningPracticeStage = ListeningPracticeStage.Loading,
    val sessionId: String? = null,
    val currentMaterialIndex: Int = 0,
    val totalMaterialCount: Int = 0,
    val currentMaterial: ListeningMaterialItem? = null,
    val currentQuestionIndexInMaterial: Int = 0,
    val currentQuestionCountInMaterial: Int = 0,
    val currentQuestionOrdinal: Int = 0,
    val totalQuestionCount: Int = 0,
    val answeredQuestionCount: Int = 0,
    val currentQuestion: ListeningQuestionItem? = null,
    val selectedOptionId: String? = null,
    val canSubmitAnswer: Boolean = false,
    val answerStatus: AnswerStatus = AnswerStatus.Unanswered,
    val feedbackMessage: String = "",
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val earnedTokens: Int = 0,
    val earnedDews: Int = 0,
    val elapsedSeconds: Int = 0,
    val showExitConfirmDialog: Boolean = false,
    val sessionOpenedAtMillis: Long = 0L,
    val errorMessage: String? = null,
    val isReviewMode: Boolean = false,
    val isFreePractice: Boolean = false
)
