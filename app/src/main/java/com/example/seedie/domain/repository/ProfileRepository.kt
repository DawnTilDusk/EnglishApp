package com.example.seedie.domain.repository

import java.time.Instant

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

    /**
     * Returns every estimate in [rangeStartInclusive, rangeEndExclusive), plus the latest
     * estimate before the range. The preceding record lets the garden interpolate the
     * beginning of a selected range without inventing a new measurement anchor.
     */
    suspend fun listMyVocabularyTrendEstimates(
        rangeStartInclusive: Instant,
        rangeEndExclusive: Instant
    ): List<VocabularyEstimateRecord>
}
