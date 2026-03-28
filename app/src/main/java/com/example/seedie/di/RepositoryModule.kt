package com.example.seedie.di

import com.example.seedie.data.repository.EconomyManagerImpl
import com.example.seedie.data.repository.UserSessionRepositoryImpl
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.UserSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserSessionRepository(
        impl: UserSessionRepositoryImpl
    ): UserSessionRepository

    @Binds
    @Singleton
    abstract fun bindEconomyManager(
        impl: EconomyManagerImpl
    ): EconomyManager
}