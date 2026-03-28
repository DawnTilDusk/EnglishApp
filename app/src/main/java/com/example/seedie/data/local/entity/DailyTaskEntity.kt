package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_tasks")
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String, // Format: "yyyy-MM-dd"
    val title: String,
    val isCompleted: Boolean = false,
    val rewardType: String = "token", // "token", "seed", "water"
    val rewardAmount: Int = 10
)