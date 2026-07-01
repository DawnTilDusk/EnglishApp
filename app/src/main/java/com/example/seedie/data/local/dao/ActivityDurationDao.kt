package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.ActivityDurationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDurationDao {
    @Query(
        """
        UPDATE activity_durations
        SET durationSec = durationSec + :durationSec, updatedAt = :updatedAt
        WHERE userId = :userId AND date = :date AND moduleId = :moduleId
        """
    )
    suspend fun incrementDuration(
        userId: String,
        date: String,
        moduleId: String,
        durationSec: Int,
        updatedAt: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ActivityDurationEntity): Long

    @Query(
        """
        SELECT moduleId, SUM(durationSec) AS durationSec
        FROM activity_durations
        WHERE userId = :userId AND date = :date
        GROUP BY moduleId
        ORDER BY durationSec DESC, moduleId ASC
        """
    )
    fun observeModuleSummariesByDate(
        userId: String,
        date: String
    ): Flow<List<ActivityModuleSummaryRow>>

    @Query(
        """
        SELECT date, SUM(durationSec) AS durationSec
        FROM activity_durations
        WHERE userId = :userId AND date BETWEEN :startDate AND :endDate
        GROUP BY date
        ORDER BY date ASC
        """
    )
    fun observeDailyTotalsBetween(
        userId: String,
        startDate: String,
        endDate: String
    ): Flow<List<DailyActivityTotalRow>>
}

data class ActivityModuleSummaryRow(
    val moduleId: String,
    val durationSec: Int
)

data class DailyActivityTotalRow(
    val date: String,
    val durationSec: Int
)
