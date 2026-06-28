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

data class ListeningQuestion(
    val questionId: String,
    val wordId: String,
    val english: String,
    val phonetic: String,
    val translation: String,
    val rewardToken: Int,
    val options: List<VocabularyPracticeOption>,
    val audioRawResId: Int
)

data class ListeningPracticeSession(
    val sessionId: String,
    val questions: List<ListeningQuestion>
)

data class ListeningPracticeUiState(
    val stage: ListeningPracticeStage = ListeningPracticeStage.Loading,
    val sessionId: String? = null,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val currentQuestion: ListeningQuestion? = null,
    val selectedOptionId: String? = null,
    val canSubmitAnswer: Boolean = false,
    val answerStatus: AnswerStatus = AnswerStatus.Unanswered,
    val feedbackMessage: String = "",
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val earnedTokens: Int = 0,
    val elapsedSeconds: Int = 0,
    val showExitConfirmDialog: Boolean = false,
    val errorMessage: String? = null
)
