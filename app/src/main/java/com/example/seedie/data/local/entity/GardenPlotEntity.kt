package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "garden_plots")
data class GardenPlotEntity(
    @PrimaryKey
    val plotIndex: Int, // 0~15 对应 4x4 网格
    val plantType: String, // 植物类型
    val level: Int // 当前等级
)