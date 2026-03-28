package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey
    val dateString: String, // 格式如 "YYYY-MM-DD"
    val studyTimeMinutes: Int,
    val isCheckedIn: Boolean
)