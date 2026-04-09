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

            // 2. 从 profiles 表获取角色和机构信息
            // 由于我们在数据库中配置了 RLS，"Users view own profile" 策略保证了当前用户只能查到自己的 profile
            val profile = client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", user.id)
                    }
                }
                .decodeSingle<Profile>()

            var studentName: String? = null
            
            // 3. 如果是学生，额外去 students 表拉取一下名字等信息
            if (profile.role == "student") {
                val student = client.postgrest["students"]
                    .select {
                        filter {
                            eq("id", user.id)
                        }
                    }
                    .decodeSingle<Student>()
                studentName = student.name
            }

            // 4. 构建并保存会话
            val session = AuthSession(
                userId = user.id,
                role = profile.role,
                agencyId = profile.agency_id,
                studentName = studentName
            )
            _currentSession.value = session

            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
