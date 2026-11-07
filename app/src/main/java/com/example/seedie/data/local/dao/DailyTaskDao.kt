package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.seedie.data.local.entity.DailyTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {
    @Query("SELECT * FROM daily_tasks WHERE userId = :userId AND date = :date")
    fun getTasksByUserAndDate(userId: String, date: String): Flow<List<DailyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTaskEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(task: DailyTaskEntity): Long

    @Query("""
        UPDATE daily_tasks SET currentCount = MAX(currentCount, :count),
            isCompleted = CASE WHEN :count >= targetCount THEN 1 ELSE isCompleted END,
            completedAt = CASE WHEN :count >= targetCount AND completedAt IS NULL THEN :now ELSE completedAt END
        WHERE userId = :userId AND date = :date AND taskKey = :key AND ruleVersion = 2
    """)
    suspend fun setProgress(userId: String, date: String, key: String, count: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE daily_tasks SET rewardGranted = 1 WHERE id = :id")
    suspend fun markRewardGranted(id: Int)

    @Query("SELECT DISTINCT date FROM daily_tasks WHERE userId = :userId AND isCompleted = 1 AND rewardGranted = 0")
    suspend fun pendingRewardDates(userId: String): List<String>

    @Query("""UPDATE daily_tasks SET isCompleted = 1, completedAt = :now
        WHERE userId = :userId AND date = :date AND taskKey = :key AND ruleVersion = 1 AND isCompleted = 0""")
    suspend fun completeLegacy(userId: String, date: String, key: String, now: Long)

    @Update
    suspend fun updateTask(task: DailyTaskEntity)
}
