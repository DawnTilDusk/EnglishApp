package com.example.seedie.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "activity_durations",
    primaryKeys = ["userId", "date", "moduleId"]
)
data class ActivityDurationEntity(
    val userId: String,
    val date: String,
    val moduleId: String,
    val durationSec: Int,
    val updatedAt: Long
)
