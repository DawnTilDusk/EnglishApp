package com.example.seedie.data.remote

import com.example.seedie.domain.model.LoginMode
import com.example.seedie.domain.model.UserRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

import com.example.seedie.data.local.DevicePreferencesRepository
import javax.inject.Inject

data class AuthSession(
    val userId: String,
    val role: UserRole,
    val agencyId: String?,
    val displayName: String?,
    val studentName: String? = null,
    val teacherId: String? = null
)

class AuthService @Inject constructor(
    private val client: SupabaseClient,
    private val devicePreferencesRepository: DevicePreferencesRepository
) {

    val sessionStatus: StateFlow<SessionStatus> = client.auth.sessionStatus

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()
    private val loginMutex = Mutex()
    private val _isLoginInProgress = MutableStateFlow(false)
    val isLoginInProgress: StateFlow<Boolean> = _isLoginInProgress.asStateFlow()
    private val _pendingLoginError = MutableStateFlow<String?>(null)
    val pendingLoginError: StateFlow<String?> = _pendingLoginError.asStateFlow()

    fun setPendingLoginError(message: String) {
        _pendingLoginError.value = message
    }

    fun consumePendingLoginError(): String? {
        val message = _pendingLoginError.value
        _pendingLoginError.value = null
        return message
    }

    fun clearLoginInProgress() {
        _isLoginInProgress.value = false
    }

    suspend fun restoreSessionFromAuth(): Result<AuthSession?> {
        if (_isLoginInProgress.value) {
            return Result.success(_currentSession.value)
        }
        return try {
            val user = client.auth.currentUserOrNull()
            if (user == null) {
                _currentSession.value = null
                return Result.success(null)
            }

            val profile = client.postgrest["profiles"]
                .select {
                    filter { eq("id", user.id) }
                }
                .decodeSingle<Profile>()

            val session = fetchBusinessSession(userId = user.id, preFetchedProfile = profile)
            _currentSession.value = session
            Result.success(session)
        } catch (e: Exception) {
            android.util.Log.e("AuthService", "restoreSessionFromAuth failed", e)
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String,
        loginMode: LoginMode,
        phone: String? = null
    ): Result<AuthSession> {
        return loginMutex.withLock {
            _isLoginInProgress.value = true
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val user = client.auth.currentUserOrNull()
                    ?: return@withLock Result.failure(Exception("Login failed: User is null"))

                val profileBefore = client.postgrest["profiles"]
                    .select {
                        filter { eq("id", user.id) }
                    }
                    .decodeSingle<Profile>()

                val role = UserRole.from(profileBefore.role)
                    ?: return@withLock Result.failure(Exception("未知账号角色"))

                when (loginMode) {
                    LoginMode.STUDENT -> {
                        if (role != UserRole.STUDENT) {
                            return@withLock Result.failure(Exception("该账号不是学生账号，请选择教师登录"))
                        }
                    }
                    LoginMode.TEACHER -> {
                        if (role != UserRole.TEACHER) {
                            return@withLock Result.failure(Exception("该账号不是教师账号，请选择学生登录"))
                        }
                    }
                }

                if (role == UserRole.STUDENT &&
                    profileBefore.phone.isNullOrBlank() &&
                    phone.isNullOrBlank()
                ) {
                    return@withLock Result.failure(Exception("请填写手机号完成绑定"))
                }

                if (!phone.isNullOrBlank() && role == UserRole.STUDENT) {
                    try {
                        syncPhoneForCurrentUser(userId = user.id, phone = phone)
                    } catch (e: Exception) {
                        return@withLock Result.failure(e)
                    }
                }

                if (role == UserRole.STUDENT) {
                    try {
                        val currentDeviceId = devicePreferencesRepository.getOrCreateDeviceId()
                        client.postgrest.rpc(
                            "set_my_device_id",
                            buildJsonObject { put("p_device_id", currentDeviceId) }
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("AuthService", "Update device_id failed", e)
                    }
                }

                val session = fetchBusinessSession(userId = user.id, preFetchedProfile = profileBefore)
                _currentSession.value = session
                _isLoginInProgress.value = false
                Result.success(session)
            } catch (e: Exception) {
                android.util.Log.e("AuthService", "login failed", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun fetchBusinessSession(userId: String, preFetchedProfile: Profile? = null): AuthSession {
        val profile = preFetchedProfile ?: client.postgrest["profiles"]
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingle<Profile>()

        val role = UserRole.from(profile.role) ?: UserRole.STUDENT
        var studentName: String? = null
        var teacherId: String? = null

        when (role) {
            UserRole.STUDENT -> {
                val student = client.postgrest["students"]
                    .select {
                        filter { eq("id", userId) }
                    }
                    .decodeSingle<Student>()
                studentName = student.name
                teacherId = student.teacher_id
            }
            UserRole.TEACHER -> {
                teacherId = userId
            }
            else -> Unit
        }

        return AuthSession(
            userId = userId,
            role = role,
            agencyId = profile.agency_id,
            displayName = profile.display_name,
            studentName = studentName,
            teacherId = teacherId
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

    suspend fun cleanupFailedLogin() {
        try {
            client.auth.signOut()
        } catch (e: Exception) {
            android.util.Log.w("AuthService", "cleanupFailedLogin signOut failed", e)
        } finally {
            _currentSession.value = null
        }
    }

    private suspend fun syncPhoneForCurrentUser(userId: String, phone: String) {
        client.postgrest.rpc(
            "set_my_phone",
            buildJsonObject { put("p_phone", phone) }
        )

        client.auth.updateUser {
            data = buildJsonObject { put("phone", phone) }
        }
    }
}
