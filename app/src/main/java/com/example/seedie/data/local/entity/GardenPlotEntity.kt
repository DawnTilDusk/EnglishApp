package com.example.seedie.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "garden_plots",
    primaryKeys = ["userId", "plotIndex"]
)
data class GardenPlotEntity(
    val userId: String,
    val plotIndex: Int,
    val plantType: String = "empty",
    val level: Int = 0,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
)