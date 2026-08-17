package com.example.seedie.domain.repository

data class UserProfile(
    val userId: String,
    val displayName: String?,
    val grade: String?,
    val email: String?,
    val phone: String?,
    val avatarToneIndex: Int,
    val vocabularySize: Int = 0,
    val hasVocabularyEstimate: Boolean = false
)

data class VocabularyEstimateRecord(
    val id: String,
    val vocabularySize: Int,
    /** ISO-8601 timestamptz from Supabase. */
    val createdAt: String
)

interface ProfileRepository {
    suspend fun getMyProfile(): UserProfile

    suspend fun updateMyProfile(
        displayName: String,
        grade: String,
        avatarToneIndex: Int
    ): Result<UserProfile>

    suspend fun bindMyPhone(phone: String): Result<UserProfile>

    suspend fun setMyVocabularyEstimate(size: Int): Result<UserProfile>

    /** Latest [limit] estimates ascending by created_at (for garden trend). */
    suspend fun listMyVocabularyEstimates(limit: Int = 30): List<VocabularyEstimateRecord>
}
