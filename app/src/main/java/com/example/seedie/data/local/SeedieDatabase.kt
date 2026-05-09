package com.example.seedie.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.seedie.data.local.dao.CheckInDao
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.dao.GardenPlotDao
import com.example.seedie.data.local.dao.VocabularyWordDao
import com.example.seedie.data.local.dao.WordBookDao
import com.example.seedie.data.local.entity.CheckInEntity
import com.example.seedie.data.local.entity.DailyTaskEntity
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.data.local.entity.GardenPlotEntity
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.WordBookEntity

@Database(
    entities = [
        DailyTaskEntity::class,
        CheckInEntity::class,
        GardenPlotEntity::class,
        EconomyTransactionEntity::class,
        WordBookEntity::class,
        VocabularyWordEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SeedieDatabase : RoomDatabase() {
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun checkInDao(): CheckInDao
    abstract fun gardenPlotDao(): GardenPlotDao
    abstract fun economyTransactionDao(): EconomyTransactionDao
    abstract fun wordBookDao(): WordBookDao
    abstract fun vocabularyWordDao(): VocabularyWordDao
}
