package com.example.seedie.domain.repository

import com.example.seedie.domain.model.PracticeAssignmentDetail
import com.example.seedie.domain.model.PracticeAssignmentListItem
import com.example.seedie.domain.model.WritingPrompt
import kotlinx.serialization.json.JsonObject

interface PracticeAssignmentRepository {
    suspend fun listForModule(moduleId: String): List<PracticeAssignmentListItem>
    suspend fun getDetail(submissionId: String): PracticeAssignmentDetail
    suspend fun start(submissionId: String)
    suspend fun submit(
        submissionId: String,
        correctCount: Int,
        totalCount: Int,
        earnedTokens: Int,
        answerPayload: JsonObject
    )
    suspend fun fetchWritingPrompt(promptId: String): WritingPrompt?
    suspend fun submitWriting(submissionId: String, originalPath: String)
    suspend fun uploadWritingOriginal(path: String, bytes: ByteArray)
    suspend fun createWritingSignedUrl(path: String): String
}
