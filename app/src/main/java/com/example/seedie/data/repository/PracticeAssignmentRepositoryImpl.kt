package com.example.seedie.data.repository

import com.example.seedie.data.remote.PracticeAssignmentRemoteDataSource
import com.example.seedie.domain.model.PracticeAssignmentDetail
import com.example.seedie.domain.model.PracticeAssignmentListItem
import com.example.seedie.domain.model.WritingPrompt
import com.example.seedie.domain.repository.PracticeAssignmentRepository
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

@Singleton
class PracticeAssignmentRepositoryImpl @Inject constructor(
    private val remote: PracticeAssignmentRemoteDataSource
) : PracticeAssignmentRepository {

    override suspend fun listForModule(moduleId: String): List<PracticeAssignmentListItem> {
        val submissions = remote.fetchMySubmissions()
        if (submissions.isEmpty()) return emptyList()
        val assignments = remote.fetchMyAssignments()
            .filter { it.module_id == moduleId }
            .associateBy { it.id }
        val itemsByAssignment = remote.fetchMyItems().groupBy { it.assignment_id }
        val now = System.currentTimeMillis()

        return submissions.mapNotNull { submission ->
            val assignment = assignments[submission.assignment_id] ?: return@mapNotNull null
            val dueMs = parseInstantMs(assignment.due_at) ?: return@mapNotNull null
            val submittedMs = submission.submitted_at?.let(::parseInstantMs)
            val turnedIn = submission.status == "submitted" || submission.status == "returned"
            val overdue = !turnedIn && now > dueMs && !assignment.allow_late
            PracticeAssignmentListItem(
                submissionId = submission.id,
                assignmentId = assignment.id,
                moduleId = assignment.module_id,
                title = assignment.title,
                dueAtEpochMs = dueMs,
                allowLate = assignment.allow_late,
                status = submission.status,
                itemCount = itemsByAssignment[assignment.id]?.size ?: 0,
                submittedAtEpochMs = submittedMs,
                correctCount = submission.correct_count,
                totalCount = submission.total_count,
                isOverdue = overdue,
                score = submission.score,
                maxScore = submission.max_score,
                earnedTokens = submission.earned_tokens
            )
        }
    }

    override suspend fun getDetail(submissionId: String): PracticeAssignmentDetail {
        val submission = remote.fetchSubmission(submissionId)
        val assignment = remote.fetchMyAssignments()
            .firstOrNull { it.id == submission.assignment_id }
            ?: error("作业不存在")
        val items = remote.fetchMyItems()
            .filter { it.assignment_id == assignment.id }
            .sortedBy { it.sort_order }
        val dueMs = parseInstantMs(assignment.due_at) ?: error("截止时间无效")
        val payload = submission.answer_payload?.let {
            runCatching { it.jsonObject }.getOrNull()
        }
        return PracticeAssignmentDetail(
            submissionId = submission.id,
            assignmentId = assignment.id,
            moduleId = assignment.module_id,
            title = assignment.title,
            dueAtEpochMs = dueMs,
            allowLate = assignment.allow_late,
            status = submission.status,
            itemRefs = items.map { it.item_ref },
            answerPayload = payload,
            correctCount = submission.correct_count,
            totalCount = submission.total_count,
            earnedTokens = submission.earned_tokens,
            score = submission.score,
            maxScore = submission.max_score,
            feedbackText = submission.feedback_text,
            originalPath = submission.original_path,
            annotatedPath = submission.annotated_path
        )
    }

    override suspend fun start(submissionId: String) {
        remote.startAssignment(submissionId)
    }

    override suspend fun submit(
        submissionId: String,
        correctCount: Int,
        totalCount: Int,
        earnedTokens: Int,
        answerPayload: JsonObject
    ) {
        remote.submitAssignment(
            submissionId = submissionId,
            correctCount = correctCount,
            totalCount = totalCount,
            earnedTokens = earnedTokens,
            answerPayload = answerPayload
        )
    }

    override suspend fun fetchWritingPrompt(promptId: String): WritingPrompt? {
        val row = remote.fetchWritingPrompts(listOf(promptId)).firstOrNull() ?: return null
        return WritingPrompt(
            promptId = row.prompt_id,
            title = row.title,
            titleZh = row.title_zh,
            promptText = row.prompt_text,
            promptTextZh = row.prompt_text_zh,
            wordCountMin = row.word_count_min,
            wordCountHint = row.word_count_hint,
            maxScore = row.max_score,
            rewardToken = row.reward_token
        )
    }

    override suspend fun submitWriting(submissionId: String, originalPath: String) {
        remote.submitWritingAssignment(submissionId, originalPath)
    }

    override suspend fun uploadWritingOriginal(path: String, bytes: ByteArray) {
        remote.uploadWritingOriginal(path, bytes)
    }

    override suspend fun createWritingSignedUrl(path: String): String {
        return remote.createWritingSignedUrl(path)
    }

    private fun parseInstantMs(raw: String): Long? {
        val candidates = listOf(
            raw,
            raw.replace(" ", "T"),
            if (raw.endsWith("Z") || raw.contains("+") || raw.count { it == '-' } > 2) {
                raw
            } else {
                "${raw.replace(" ", "T")}Z"
            }
        )
        for (candidate in candidates) {
            try {
                return Instant.parse(candidate).toEpochMilli()
            } catch (_: DateTimeParseException) {
                // try next
            }
        }
        return null
    }
}
