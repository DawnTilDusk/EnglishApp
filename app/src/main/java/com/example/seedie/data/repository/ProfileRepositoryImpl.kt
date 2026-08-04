package com.example.seedie.data.repository

import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.Profile
import com.example.seedie.domain.profile.ProfileGradeOptions
import com.example.seedie.domain.repository.ProfileRepository
import com.example.seedie.domain.repository.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
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
