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
    val isOverdue: Boolean
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
    val earnedTokens: Int
)
