package com.example.seedie.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import javax.inject.Inject

data class AuthSession(
    val userId: String,
    val role: String,
    val agencyId: String?,
    val studentName: String? = null
)

class AuthService @Inject constructor(private val client: SupabaseClient) {

    val sessionStatus: StateFlow<SessionStatus> = client.auth.sessionStatus

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
    suspend fun login(email: String, password: String, phone: String? = null): Result<AuthSession> {
        return try {
            // 1. 调用 Supabase Auth 进行登录
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            val user = client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Login failed: User is null"))

            val profileBefore = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", user.id)
                    }
                }
                .decodeSingle<Profile>()

            if (profileBefore.role == "student" && profileBefore.phone.isNullOrBlank() && phone.isNullOrBlank()) {
                client.auth.signOut()
                _currentSession.value = null
                return Result.failure(Exception("请填写手机号完成绑定"))
            }

            if (!phone.isNullOrBlank()) {
                try {
                    syncPhoneForCurrentUser(userId = user.id, phone = phone)
                } catch (e: Exception) {
                    client.auth.signOut()
                    _currentSession.value = null
                    return Result.failure(e)
                }
            }

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

    private suspend fun syncPhoneForCurrentUser(userId: String, phone: String) {
        client.postgrest.rpc(
            "set_my_phone",
            buildJsonObject {
                put("p_phone", phone)
            }
        )

        client.auth.updateUser {
            data = buildJsonObject {
                put("phone", phone)
            }
        }
    }
}
