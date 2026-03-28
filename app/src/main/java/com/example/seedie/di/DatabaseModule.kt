package com.example.seedie.di

import android.content.Context
import androidx.room.Room
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.dao.CheckInDao
import com.example.seedie.data.local.dao.DailyTaskDao
import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.dao.GardenPlotDao
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
    fun provideDailyTaskDao(database: SeedieDatabase): DailyTaskDao = database.dailyTaskDao()

    @Provides
    fun provideCheckInDao(database: SeedieDatabase): CheckInDao = database.checkInDao()

    @Provides
    fun provideGardenPlotDao(database: SeedieDatabase): GardenPlotDao = database.gardenPlotDao()

    @Provides
    fun provideEconomyTransactionDao(database: SeedieDatabase): EconomyTransactionDao = database.economyTransactionDao()
}