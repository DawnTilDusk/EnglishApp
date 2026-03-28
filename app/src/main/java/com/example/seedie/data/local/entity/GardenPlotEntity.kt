package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "garden_plots")
data class GardenPlotEntity(
    @PrimaryKey
    val plotIndex: Int, // 0 to 15 (for a 4x4 grid)
    val plantType: String = "empty", // "empty", "grass", "flower"
    val level: Int = 0 // 0: seed, 1: sprout, 2: blooming
)