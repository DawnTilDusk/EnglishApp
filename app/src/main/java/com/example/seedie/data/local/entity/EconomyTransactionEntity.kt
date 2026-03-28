package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "economy_transactions")
data class EconomyTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val amount: Int, // 正数代表获得，负数代表消耗
    val reason: String // 变更原因，如 "Completed Daily Task", "Bought Seed"
)