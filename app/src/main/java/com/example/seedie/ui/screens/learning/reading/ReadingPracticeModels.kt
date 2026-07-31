package com.example.seedie.ui.screens.learning.reading

import com.example.seedie.ui.screens.learning.practice.AnswerStatus
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption

enum class ReadingPracticeStage {
    Loading,
    Answering,
    Reviewing,
    Completed,
    Error
}

data class ReadingQuestionItem(
    val questionId: String,
    val questionType: String?,
    val stem: String,
    val options: List<VocabularyPracticeOption>,
    val correctOptionId: String,
    val explanation: String,
    val highlightWord: String?,
    val rewardToken: Int
)

data class ReadingSetItem(
    val setId: String,
    val title: String,
    val titleZh: String?,
    val passage: String,
    val questions: List<ReadingQuestionItem>
)

data class ReadingPracticeSession(
    val sessionId: String,
    val sets: List<ReadingSetItem>
)

data class ReadingPracticeUiState(
    val stage: ReadingPracticeStage = ReadingPracticeStage.Loading,
    val sessionId: String? = null,
    val currentSetIndex: Int = 0,
    val totalSetCount: Int = 0,
    val currentSet: ReadingSetItem? = null,
    val answers: Map<String, String> = emptyMap(),
    val canSubmitSet: Boolean = false,
    val submitHint: String = "",
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val earnedTokens: Int = 0,
    val elapsedSeconds: Int = 0,
    val showExitConfirmDialog: Boolean = false,
    val errorMessage: String? = null,
    val isReviewMode: Boolean = false
) {
    fun answerStatusFor(question: ReadingQuestionItem): AnswerStatus {
        if (stage != ReadingPracticeStage.Reviewing) return AnswerStatus.Unanswered
        val selected = answers[question.questionId] ?: return AnswerStatus.Unanswered
        return if (selected == question.correctOptionId) AnswerStatus.Correct else AnswerStatus.Wrong
    }
}
