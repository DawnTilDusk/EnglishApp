package com.example.seedie.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.seedie.data.local.dao.CheckInDao
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.dao.GardenPlotDao
import com.example.seedie.data.local.entity.CheckInEntity
import com.example.seedie.data.local.entity.DailyTaskEntity
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.data.local.entity.GardenPlotEntity

@Database(
    entities = [
        DailyTaskEntity::class,
        CheckInEntity::class,
        GardenPlotEntity::class,
        EconomyTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SeedieDatabase : RoomDatabase() {
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun checkInDao(): CheckInDao
    abstract fun gardenPlotDao(): GardenPlotDao
    abstract fun economyTransactionDao(): EconomyTransactionDao
}