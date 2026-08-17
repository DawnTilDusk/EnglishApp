package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins WHERE userId = :userId ORDER BY date DESC")
    fun getAllCheckIns(userId: String): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getCheckInByDate(userId: String, date: String): CheckInEntity?

    @Query(
        "SELECT * FROM check_ins WHERE userId = :userId AND date <= :date " +
            "ORDER BY date DESC LIMIT 30"
    )
    suspend fun getRecentCheckIns(userId: String, date: String): List<CheckInEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCheckIn(checkIn: CheckInEntity)

    @Query("SELECT * FROM check_ins WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingCheckIns(userId: String): List<CheckInEntity>

    @Query("UPDATE check_ins SET syncStatus = :status, syncedAt = :syncedAt WHERE userId = :userId AND date = :date")
    suspend fun updateSyncStatus(userId: String, date: String, status: String, syncedAt: Long?)
}