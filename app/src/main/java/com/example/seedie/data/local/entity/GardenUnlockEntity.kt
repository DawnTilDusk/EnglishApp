package com.example.seedie.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "garden_unlocks",
    primaryKeys = ["userId", "speciesId"]
)
data class GardenUnlockEntity(
    val userId: String,
    val speciesId: String,
    val unlockedAt: Long
)
