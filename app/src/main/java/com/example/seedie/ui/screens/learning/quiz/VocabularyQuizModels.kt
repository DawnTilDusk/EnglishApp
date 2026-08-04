package com.example.seedie.ui.screens.learning.quiz

import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption

enum class VocabularyQuizStage {
    Loading,
    Ready,
    AnswerEvaluated,
    Completed,
    Error
}

data class VocabularyQuizQuestion(
    val questionId: String,
    val wordId: String,
    val english: String,
    val phonetic: String,
    val partOfSpeech: String,
    val translation: String,
    val difficultyLevel: String,
    val rewardToken: Int,
    val options: List<VocabularyPracticeOption>
)

data class VocabularyQuizSession(
    val sessionId: String,
    val questions: List<VocabularyQuizQuestion>
)

data class VocabularyQuizWrongWord(
    val wordId: String,
    val english: String,
    val translation: String
)

data class VocabularyQuizUiState(
    val stage: VocabularyQuizStage = VocabularyQuizStage.Loading,
    val sessionId: String? = null,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val currentQuestion: VocabularyQuizQuestion? = null,
    val selectedOptionId: String? = null,
    val canSubmitAnswer: Boolean = false,
    val answerStatus: com.example.seedie.ui.screens.learning.practice.AnswerStatus =
        com.example.seedie.ui.screens.learning.practice.AnswerStatus.Unanswered,
    val feedbackMessage: String = "",
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val earnedTokens: Int = 0,
    val elapsedSeconds: Int = 0,
    val estimatedVocabulary: Int? = null,
    val wrongWords: List<VocabularyQuizWrongWord> = emptyList(),
    val showExitConfirmDialog: Boolean = false,
    val errorMessage: String? = null,
    val bandIndex: Int = 0,
    val bandCount: Int = 0,
    val bandTitle: String = "",
    val bandProgressLabel: String = ""
)
