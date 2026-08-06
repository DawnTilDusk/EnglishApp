package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "garden_plants",
    primaryKeys = ["id"],
    indices = [
        Index(value = ["userId", "sessionId"], unique = true),
        Index(value = ["userId", "localDate"])
    ]
)
data class GardenPlantEntity(
    val id: String,
    val userId: String,
    val sessionId: String,
    val moduleId: String,
    val speciesId: String,
    val status: String,
    val completedQuestionCount: Int,
    val correctCount: Int,
    val studyDurationSec: Int,
    val createdAt: Long,
    val localDate: String,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
) {
    companion object {
        const val STATUS_ALIVE = "ALIVE"
        const val STATUS_WITHERED = "WITHERED"
    }
}
