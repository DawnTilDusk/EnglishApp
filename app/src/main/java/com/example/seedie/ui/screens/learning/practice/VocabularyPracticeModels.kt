package com.example.seedie.ui.screens.learning.practice

data class VocabularyPracticeArgs(
    val sessionId: String? = null,
    val sourceModuleId: String = "vocabulary",
    val planId: String? = null,
    val wordCountTarget: Int = 5,
    val difficulty: String = "easy",
    val resumeToken: String? = null
)

enum class VocabularyPracticeStage {
    Loading,
    Ready,
    AnswerEvaluated,
    Completed,
    Empty,
    Error
}

enum class VocabularyQuestionType {
    MULTIPLE_CHOICE_TRANSLATION
}

enum class AnswerStatus {
    Unanswered,
    Correct,
    Wrong,
    Skipped
}

data class VocabularyPracticeOption(
    val optionId: String,
    val label: String,
    val isCorrect: Boolean
)

data class VocabularyPracticeQuestion(
    val questionId: String,
    val wordId: String,
    val questionType: VocabularyQuestionType,
    val english: String,
    val phonetic: String,
    val partOfSpeech: String,
    val translationCorrect: String,
    val optionList: List<VocabularyPracticeOption>,
    val exampleSentence: String,
    val difficultyLevel: String,
    val rewardToken: Int,
    val estimatedDurationSec: Int
)

data class VocabularySessionMeta(
    val sessionId: String,
    val moduleId: String,
    val startedAt: Long,
    val targetWordCount: Int,
    val difficulty: String,
    val source: String,
    val resumeSupported: Boolean
)

data class VocabularyPracticeSession(
    val sessionMeta: VocabularySessionMeta,
    val questions: List<VocabularyPracticeQuestion>
)

data class VocabularyQuestionRecord(
    val sessionId: String,
    val questionId: String,
    val wordId: String,
    val selectedOptionId: String?,
    val isCorrect: Boolean,
    val isSkipped: Boolean,
    val elapsedSeconds: Int
)

data class VocabularyPracticeUiState(
    val stage: VocabularyPracticeStage = VocabularyPracticeStage.Loading,
    val sessionMeta: VocabularySessionMeta? = null,
    val questions: List<VocabularyPracticeQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val currentQuestion: VocabularyPracticeQuestion? = null,
    val selectedOptionId: String? = null,
    val answerStatus: AnswerStatus = AnswerStatus.Unanswered,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val skippedCount: Int = 0,
    val earnedTokens: Int = 0,
    val elapsedSeconds: Int = 0,
    val remainingCount: Int = 0,
    val canSubmitAnswer: Boolean = false,
    val canGoNext: Boolean = false,
    val showExitConfirmDialog: Boolean = false,
    val showFinishDialog: Boolean = false,
    val errorMessage: String? = null
)
