package com.example.seedie.domain.model

data class StudyResult(
    val sessionId: String,
    val moduleId: String,
    val isCompleted: Boolean,
    val completedQuestionCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val accuracy: Float,
    val earnedTokens: Int,
    val studyDurationSec: Int,
    val vocabularyDelta: Int,
    val wrongWordIds: List<String>
) {
    val studyTimeMinutes: Int
        get() = if (studyDurationSec <= 0) 0 else (studyDurationSec + 59) / 60

    val newVocabularyCount: Int
        get() = vocabularyDelta
}
