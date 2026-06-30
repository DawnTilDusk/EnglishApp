package com.example.seedie.domain.repository

data class UserProfile(
    val userId: String,
    val displayName: String?,
    val grade: String?,
    val email: String?,
    val phone: String?,
    val avatarToneIndex: Int
)

interface ProfileRepository {
    suspend fun getMyProfile(): UserProfile

    suspend fun updateMyProfile(
        displayName: String,
        grade: String,
        avatarToneIndex: Int
    ): Result<UserProfile>

    suspend fun bindMyPhone(phone: String): Result<UserProfile>
}
