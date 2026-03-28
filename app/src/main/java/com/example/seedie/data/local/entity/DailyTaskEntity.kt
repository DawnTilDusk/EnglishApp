package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_tasks")
data class DailyTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long, // 任务所属日期（如时间戳）
    val title: String,
    val isCompleted: Boolean = false,
    val rewardType: String, // 奖励类型（如：tokens, seed, water）
    val rewardAmount: Int
)