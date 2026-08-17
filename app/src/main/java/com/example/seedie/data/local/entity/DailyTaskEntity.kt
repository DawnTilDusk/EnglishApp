package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_tasks",
    indices = [
        Index(value = ["userId", "date"], name = "index_daily_tasks_userId_date"),
        Index(value = ["userId", "date", "taskKey"], unique = true,
            name = "index_daily_tasks_userId_date_taskKey")
    ]
)
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val date: String,
    val title: String,
    val isCompleted: Boolean = false,
    val rewardType: String = "dew",
    val rewardAmount: Int = 5,
    val tokenReward: Int = 0,
    val autoClaim: Boolean = true,
    val completedAt: Long? = null,
    val taskKey: String = ""
)
