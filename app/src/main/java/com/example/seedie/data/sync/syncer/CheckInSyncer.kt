package com.example.seedie.data.sync.syncer

import com.example.seedie.data.local.dao.CheckInDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CheckInSyncer @Inject constructor(
    private val checkInDao: CheckInDao,
    private val client: SupabaseClient
) {
    suspend fun sync(userId: String) {
        val pending = checkInDao.getPendingCheckIns(userId)
        if (pending.isEmpty()) return

        pending.forEach { checkIn ->
            try {
                client.postgrest["user_check_ins"].upsert(
                    UserCheckInDto(
                        user_id = userId,
                        date = checkIn.date,
                        is_checked_in = checkIn.isCheckedIn,
                        study_time_minutes = checkIn.studyTimeMinutes
                    )
                )
                checkInDao.updateSyncStatus(
                    userId = userId,
                    date = checkIn.date,
                    status = "SYNCED",
                    syncedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                android.util.Log.e("CheckInSyncer", "Failed to sync check-in ${checkIn.date}", e)
            }
        }
    }
}

@Serializable
data class UserCheckInDto(
    val user_id: String,
    val date: String,
    val is_checked_in: Boolean,
    val study_time_minutes: Int
)
