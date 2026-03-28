package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "economy_transactions")
data class EconomyTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val amount: Int, // Positive for earning, negative for spending
    val reason: String // e.g., "Completed Daily Task", "Bought Seed"
)