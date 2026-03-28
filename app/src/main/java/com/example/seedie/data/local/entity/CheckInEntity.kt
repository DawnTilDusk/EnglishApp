package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_ins")
data class CheckInEntity(
    @PrimaryKey
    val date: String, // Format: "yyyy-MM-dd"
    val isCheckedIn: Boolean = false,
    val studyTimeMinutes: Int = 0
)