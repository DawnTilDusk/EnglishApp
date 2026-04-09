package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import javax.inject.Inject

data class AuthSession(
    val userId: String,
    val role: String,
    val agencyId: String?,
    val studentName: String? = null
)

class AuthService @Inject constructor(private val client: SupabaseClient) {

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    suspend fun restoreSessionFromAuth(): Result<AuthSession?> {
        return try {
            val user = client.auth.currentUserOrNull()
            if (user == null) {
                _currentSession.value = null
                return Result.success(null)
            }

            val session = fetchBusinessSession(userId = user.id)
            _currentSession.value = session
            Result.success(session)
        } catch (e: Exception) {
            _currentSession.value = null
            Result.failure(e)
        }
    }

    /**
     * 登录并获取用户的身份和角色
     */
    suspend fun login(email: String, password: String): Result<AuthSession> {
        return try {
            // 1. 调用 Supabase Auth 进行登录
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val user = client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Login failed: User is null"))

            val session = fetchBusinessSession(userId = user.id)
            _currentSession.value = session

            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchBusinessSession(userId: String): AuthSession {
        val profile = client.postgrest["profiles"]
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<Profile>()

        var studentName: String? = null

        if (profile.role == "student") {
            val student = client.postgrest["students"]
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<Student>()
            studentName = student.name
        }

        return AuthSession(
            userId = userId,
            role = profile.role,
            agencyId = profile.agency_id,
            studentName = studentName
        )
    }

    suspend fun logout() {
        try {
            client.auth.signOut()
            _currentSession.value = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
