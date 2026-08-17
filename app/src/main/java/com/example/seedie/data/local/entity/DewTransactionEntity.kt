package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dew_transactions",
    indices = [
        Index(
            value = ["userId", "refId"],
            unique = true,
            name = "index_dew_transactions_userId_refId"
        ),
        Index(
            value = ["userId", "timestamp"],
            name = "index_dew_transactions_userId_timestamp"
        )
    ]
)
data class DewTransactionEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val timestamp: Long,
    val amount: Int,
    val reason: String,
    val refId: String? = null,
    val syncStatus: String = "LOCAL_ONLY",
    val syncedAt: Long? = null
)
