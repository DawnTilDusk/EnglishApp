package com.example.seedie.domain.model

import kotlinx.serialization.json.JsonObject

enum class PracticeAssignmentMode {
    Answer,
    Review
}

data class PracticeAssignmentListItem(
    val submissionId: String,
    val assignmentId: String,
    val moduleId: String,
    val title: String,
    val dueAtEpochMs: Long,
    val allowLate: Boolean,
    val status: String,
    val itemCount: Int,
    val submittedAtEpochMs: Long?,
    val correctCount: Int,
    val totalCount: Int,
    val isOverdue: Boolean,
    val score: Int? = null,
    val maxScore: Int? = null,
    val earnedTokens: Int = 0
)

data class PracticeAssignmentDetail(
    val submissionId: String,
    val assignmentId: String,
    val moduleId: String,
    val title: String,
    val dueAtEpochMs: Long,
    val allowLate: Boolean,
    val status: String,
    val itemRefs: List<String>,
    val answerPayload: JsonObject?,
    val correctCount: Int,
    val totalCount: Int,
    val earnedTokens: Int,
    val score: Int? = null,
    val maxScore: Int? = null,
    val feedbackText: String? = null,
    val originalPath: String? = null,
    val annotatedPath: String? = null
)

data class WritingPrompt(
    val promptId: String,
    val title: String,
    val titleZh: String?,
    val promptText: String,
    val promptTextZh: String?,
    val wordCountMin: Int,
    val wordCountHint: Int,
    val maxScore: Int,
    val rewardToken: Int
)
