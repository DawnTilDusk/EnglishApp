package com.example.seedie.di

import android.content.Context
import androidx.room.Room
import com.example.seedie.data.local.SeedieDatabase
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
        ).build()
    }

    @Provides
    @Singleton
    fun provideDailyTaskDao(database: SeedieDatabase) = database.dailyTaskDao()

    @Provides
    @Singleton
    fun provideCheckInDao(database: SeedieDatabase) = database.checkInDao()

    @Provides
    @Singleton
    fun provideGardenPlotDao(database: SeedieDatabase) = database.gardenPlotDao()

    @Provides
    @Singleton
    fun provideEconomyTransactionDao(database: SeedieDatabase) = database.economyTransactionDao()
}