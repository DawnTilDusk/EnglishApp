package com.example.seedie.data.repository

import android.util.Log
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.Profile
import com.example.seedie.data.remote.UserVocabularyEstimate
import com.example.seedie.domain.profile.ProfileGradeOptions
import com.example.seedie.domain.repository.ProfileRepository
import com.example.seedie.domain.repository.UserProfile
import com.example.seedie.domain.repository.VocabularyEstimateRecord
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val authService: AuthService
) : ProfileRepository {

    private val phonePattern = Regex("^\\+?[0-9]{11,13}$")

    override suspend fun getMyProfile(): UserProfile {
        val userId = requireCurrentUserId()
        return fetchProfile(userId)
    }

    override suspend fun updateMyProfile(
        displayName: String,
        grade: String,
        avatarToneIndex: Int
    ): Result<UserProfile> = runCatching {
        require(avatarToneIndex in 0..2) { "头像样式无效" }
        val normalizedGrade = ProfileGradeOptions.normalize(grade)

        client.postgrest.rpc(
            "set_my_profile",
            buildJsonObject {
                put("p_display_name", displayName.trim())
                put("p_grade", normalizedGrade)
                put("p_avatar_tone", avatarToneIndex)
            }
        )

        authService.refreshBusinessSession().getOrThrow()
        getMyProfile()
    }

    override suspend fun bindMyPhone(phone: String): Result<UserProfile> = runCatching {
        val normalizedPhone = normalizePhone(phone)
        require(phonePattern.matches(normalizedPhone)) { "手机号格式不正确" }

        client.postgrest.rpc(
            "set_my_phone",
            buildJsonObject { put("p_phone", normalizedPhone) }
        )

        client.auth.updateUser {
            data = buildJsonObject { put("phone", normalizedPhone) }
        }

        getMyProfile()
    }

    override suspend fun setMyVocabularyEstimate(size: Int): Result<UserProfile> = runCatching {
        require(size >= 0) { "词汇量不能为负数" }
        client.postgrest.rpc(
            "set_my_vocabulary_estimate",
            buildJsonObject { put("p_size", size) }
        )
        getMyProfile()
    }

    override suspend fun listMyVocabularyEstimates(limit: Int): List<VocabularyEstimateRecord> {
        val userId = requireCurrentSupabaseUserId()
        val capped = limit.coerceIn(1, 100)
        // Fetch newest first then reverse so chart is chronological with at most [capped] points.
        val rows = client.postgrest["user_vocabulary_estimates"]
            .select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(capped.toLong())
            }
            .decodeList<UserVocabularyEstimate>()
        return rows
            .asReversed()
            .map { row ->
                VocabularyEstimateRecord(
                    id = row.id,
                    vocabularySize = row.vocabulary_size,
                    createdAt = row.created_at
                )
            }
    }

    override suspend fun listMyVocabularyTrendEstimates(
        rangeStartInclusive: Instant,
        rangeEndExclusive: Instant
    ): List<VocabularyEstimateRecord> {
        require(rangeStartInclusive.isBefore(rangeEndExclusive)) {
            "Vocabulary trend range must not be empty"
        }
        val userId = requireCurrentSupabaseUserId()
        val rangeStart = rangeStartInclusive.toString()
        val rangeEnd = rangeEndExclusive.toString()
        Log.i(
            "VocabularyTrend",
            "Repository starting Supabase query: range=$rangeStart..$rangeEnd"
        )

        val previousRows = client.postgrest["user_vocabulary_estimates"]
            .select {
                filter {
                    eq("user_id", userId)
                    lt("created_at", rangeStart)
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<UserVocabularyEstimate>()

        val rangeRows = client.postgrest["user_vocabulary_estimates"]
            .select {
                filter {
                    eq("user_id", userId)
                    and {
                        gte("created_at", rangeStart)
                        lt("created_at", rangeEnd)
                    }
                }
                order("created_at", Order.ASCENDING)
            }
            .decodeList<UserVocabularyEstimate>()

        return (previousRows + rangeRows)
            .distinctBy { it.id }
            .sortedBy { it.created_at }
            .map { row ->
                VocabularyEstimateRecord(
                    id = row.id,
                    vocabularySize = row.vocabulary_size,
                    createdAt = row.created_at
                )
            }
    }

    private suspend fun fetchProfile(userId: String): UserProfile {
        val profile = client.postgrest["profiles"]
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle<Profile>()

        return profile.toUserProfile()
    }

    private suspend fun requireCurrentUserId(): String {
        return authService.currentSession.value?.userId
            ?: client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("当前未登录")
    }

    /**
     * Data API queries must use the user represented by the current JWT. The business-session
     * cache can be temporarily stale during session restoration or after switching accounts.
     */
    private suspend fun requireCurrentSupabaseUserId(): String {
        return client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("当前未登录")
    }

    private fun normalizePhone(input: String): String {
        return input.trim().replace(" ", "").replace("-", "")
    }
}

private fun Profile.toUserProfile(): UserProfile {
    return UserProfile(
        userId = id,
        displayName = display_name,
        grade = grade,
        email = email,
        phone = phone,
        avatarToneIndex = avatar_tone,
        vocabularySize = vocabulary_size,
        hasVocabularyEstimate = vocabulary_estimated_at != null
    )
}
