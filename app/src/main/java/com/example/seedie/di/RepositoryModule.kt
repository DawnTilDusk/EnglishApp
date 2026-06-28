package com.example.seedie.di

import com.example.seedie.data.repository.EconomyManagerImpl
import com.example.seedie.data.repository.UserSessionRepositoryImpl
import com.example.seedie.data.repository.ListeningPracticeRepositoryImpl
import com.example.seedie.data.repository.VocabularyPracticeRepositoryImpl
import com.example.seedie.data.repository.VocabularyQuizRepositoryImpl
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.ListeningPracticeRepository
import com.example.seedie.domain.repository.UserSessionRepository
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import com.example.seedie.domain.repository.VocabularyQuizRepository
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
        userSessionRepositoryImpl: UserSessionRepositoryImpl
    ): UserSessionRepository

    @Binds
    @Singleton
    abstract fun bindEconomyManager(
        economyManagerImpl: EconomyManagerImpl
    ): EconomyManager

    @Binds
    @Singleton
    abstract fun bindVocabularyPracticeRepository(
        vocabularyPracticeRepositoryImpl: VocabularyPracticeRepositoryImpl
    ): VocabularyPracticeRepository

    @Binds
    @Singleton
    abstract fun bindListeningPracticeRepository(
        listeningPracticeRepositoryImpl: ListeningPracticeRepositoryImpl
    ): ListeningPracticeRepository

    @Binds
    @Singleton
    abstract fun bindVocabularyQuizRepository(
        vocabularyQuizRepositoryImpl: VocabularyQuizRepositoryImpl
    ): VocabularyQuizRepository
}
