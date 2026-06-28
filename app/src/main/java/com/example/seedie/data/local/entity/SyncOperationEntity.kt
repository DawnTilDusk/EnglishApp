package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey val operationId: String,
    val userId: String,
    val tableName: String,
    val operationType: String,  // UPSERT | DELETE
    val payload: String,        // JSON
    val createdAt: Long,
    val retryCount: Int = 0,
    val status: String = "PENDING"  // PENDING | FAILED
)
