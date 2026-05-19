package com.example.seedie.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.seedie.data.local.dao.CheckInDao
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.dao.GardenPlotDao
import com.example.seedie.data.local.dao.SyncOperationDao
import com.example.seedie.data.local.dao.VocabularyBookProgressDao
import com.example.seedie.data.local.dao.VocabularyStudyRoundDao
import com.example.seedie.data.local.dao.VocabularyStudyRoundWordDao
import com.example.seedie.data.local.dao.VocabularyWordDao
import com.example.seedie.data.local.dao.VocabularyWordLearningProgressDao
import com.example.seedie.data.local.dao.WordBookDao
import com.example.seedie.data.local.entity.CheckInEntity
import com.example.seedie.data.local.entity.DailyTaskEntity
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.data.local.entity.GardenPlotEntity
import com.example.seedie.data.local.entity.SyncOperationEntity
import com.example.seedie.data.local.entity.VocabularyBookProgressEntity
import com.example.seedie.data.local.entity.VocabularyStudyRoundEntity
import com.example.seedie.data.local.entity.VocabularyStudyRoundWordEntity
import com.example.seedie.data.local.entity.VocabularyWordEntity
import com.example.seedie.data.local.entity.VocabularyWordLearningProgressEntity
import com.example.seedie.data.local.entity.WordBookEntity

@Database(
    entities = [
        DailyTaskEntity::class,
        CheckInEntity::class,
        GardenPlotEntity::class,
        EconomyTransactionEntity::class,
        WordBookEntity::class,
        VocabularyWordEntity::class,
        VocabularyBookProgressEntity::class,
        VocabularyWordLearningProgressEntity::class,
        VocabularyStudyRoundEntity::class,
        VocabularyStudyRoundWordEntity::class,
        SyncOperationEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SeedieDatabase : RoomDatabase() {
    abstract fun dailyTaskDao(): DailyTaskDao
    abstract fun checkInDao(): CheckInDao
    abstract fun gardenPlotDao(): GardenPlotDao
    abstract fun economyTransactionDao(): EconomyTransactionDao
    abstract fun wordBookDao(): WordBookDao
    abstract fun vocabularyWordDao(): VocabularyWordDao
    abstract fun vocabularyBookProgressDao(): VocabularyBookProgressDao
    abstract fun vocabularyWordLearningProgressDao(): VocabularyWordLearningProgressDao
    abstract fun vocabularyStudyRoundDao(): VocabularyStudyRoundDao
    abstract fun vocabularyStudyRoundWordDao(): VocabularyStudyRoundWordDao
    abstract fun syncOperationDao(): SyncOperationDao
}
