package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "economy_transactions",
    indices = [
        Index(
            value = ["userId", "refId"],
            unique = true,
            name = "index_economy_transactions_userId_refId"
        )
    ]
)
data class EconomyTransactionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val timestamp: Long,
    val amount: Int,
    val reason: String,
    val refId: String? = null,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
)
