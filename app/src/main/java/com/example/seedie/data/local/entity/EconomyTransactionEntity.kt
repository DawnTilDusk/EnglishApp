package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "economy_transactions")
data class EconomyTransactionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val timestamp: Long,
    val amount: Int,
    val reason: String,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
)