package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Singleton
class PracticeAssignmentRemoteDataSource @Inject constructor(
    private val client: SupabaseClient
) {
    suspend fun fetchMySubmissions(): List<SupabasePracticeAssignmentSubmission> {
        return client.postgrest["practice_assignment_submissions"].select {
            order("submitted_at", Order.DESCENDING)
        }.decodeList()
    }

    suspend fun fetchMyAssignments(): List<SupabasePracticeAssignment> {
        return client.postgrest["practice_assignments"].select {
            order("due_at", Order.ASCENDING)
        }.decodeList()
    }

    suspend fun fetchMyItems(): List<SupabasePracticeAssignmentItem> {
        return client.postgrest["practice_assignment_items"].select {
            order("sort_order", Order.ASCENDING)
        }.decodeList()
    }

    suspend fun fetchSubmission(submissionId: String): SupabasePracticeAssignmentSubmission {
        return client.postgrest["practice_assignment_submissions"].select {
            filter { eq("id", submissionId) }
        }.decodeSingle()
    }

    suspend fun startAssignment(submissionId: String): JsonElement {
        return client.postgrest.rpc(
            "start_practice_assignment",
            buildJsonObject { put("p_submission_id", submissionId) }
        ).decodeAs()
    }

    suspend fun submitAssignment(
        submissionId: String,
        correctCount: Int,
        totalCount: Int,
        earnedTokens: Int,
        answerPayload: JsonObject
    ): JsonElement {
        return client.postgrest.rpc(
            "submit_practice_assignment",
            buildJsonObject {
                put("p_submission_id", submissionId)
                put("p_correct_count", correctCount)
                put("p_total_count", totalCount)
                put("p_earned_tokens", earnedTokens)
                put("p_answer_payload", answerPayload)
            }
        ).decodeAs()
    }
}
