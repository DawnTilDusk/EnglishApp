package com.example.seedie.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "check_ins",
    primaryKeys = ["userId", "date"]
)
data class CheckInEntity(
    val userId: String,
    val date: String,
    val isCheckedIn: Boolean = false,
    val studyTimeMinutes: Int = 0,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
)