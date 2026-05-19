package com.example.seedie.di

import android.content.Context
import androidx.room.Room
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.SeedieDatabaseMigrations
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSeedieDatabase(
        @ApplicationContext context: Context
    ): SeedieDatabase {
        return Room.databaseBuilder(
            context,
            SeedieDatabase::class.java,
            "seedie_database"
        )
            .addMigrations(SeedieDatabaseMigrations.MIGRATION_1_2)
            .addMigrations(SeedieDatabaseMigrations.MIGRATION_2_3)
            .addMigrations(SeedieDatabaseMigrations.MIGRATION_3_4)
            .addMigrations(SeedieDatabaseMigrations.MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideDailyTaskDao(database: SeedieDatabase): DailyTaskDao = database.dailyTaskDao()

    @Provides
    fun provideCheckInDao(database: SeedieDatabase): CheckInDao = database.checkInDao()

    @Provides
    fun provideGardenPlotDao(database: SeedieDatabase): GardenPlotDao = database.gardenPlotDao()

    @Provides
    fun provideEconomyTransactionDao(database: SeedieDatabase): EconomyTransactionDao = database.economyTransactionDao()

    @Provides
    fun provideWordBookDao(database: SeedieDatabase): WordBookDao = database.wordBookDao()

    @Provides
    fun provideVocabularyWordDao(database: SeedieDatabase): VocabularyWordDao = database.vocabularyWordDao()

    @Provides
    fun provideVocabularyBookProgressDao(
        database: SeedieDatabase
    ): VocabularyBookProgressDao = database.vocabularyBookProgressDao()

    @Provides
    fun provideVocabularyWordLearningProgressDao(
        database: SeedieDatabase
    ): VocabularyWordLearningProgressDao = database.vocabularyWordLearningProgressDao()

    @Provides
    fun provideVocabularyStudyRoundDao(
        database: SeedieDatabase
    ): VocabularyStudyRoundDao = database.vocabularyStudyRoundDao()

    @Provides
    fun provideVocabularyStudyRoundWordDao(
        database: SeedieDatabase
    ): VocabularyStudyRoundWordDao = database.vocabularyStudyRoundWordDao()

    @Provides
    fun provideSyncOperationDao(database: SeedieDatabase): SyncOperationDao = database.syncOperationDao()
}
