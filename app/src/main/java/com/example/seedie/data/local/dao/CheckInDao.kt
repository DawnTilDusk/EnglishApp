package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins ORDER BY dateString ASC")
    fun getAllCheckIns(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE dateString = :dateString LIMIT 1")
    suspend fun getCheckInByDate(dateString: String): CheckInEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckIn(checkIn: CheckInEntity)
}