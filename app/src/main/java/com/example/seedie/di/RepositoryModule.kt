package com.example.seedie.di

import com.example.seedie.data.remote.EconomyCloudGateway
import com.example.seedie.data.remote.EconomyRemoteDataSource
import com.example.seedie.data.repository.ActivityTrackingRepositoryImpl
import com.example.seedie.data.repository.EconomyManagerImpl
import com.example.seedie.data.repository.ListeningPracticeRepositoryImpl
import com.example.seedie.data.repository.ProfileRepositoryImpl
import com.example.seedie.data.repository.ShopRepositoryImpl
import com.example.seedie.data.repository.TeacherRepositoryImpl
import com.example.seedie.data.repository.UserSessionRepositoryImpl
import com.example.seedie.data.repository.VocabularyPracticeRepositoryImpl
import com.example.seedie.data.repository.VocabularyQuizRepositoryImpl
import com.example.seedie.data.repository.WordBookRepositoryImpl
import com.example.seedie.domain.repository.ActivityTrackingRepository
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.ListeningPracticeRepository
import com.example.seedie.domain.repository.ProfileRepository
import com.example.seedie.domain.repository.ShopRepository
import com.example.seedie.domain.repository.TeacherRepository
import com.example.seedie.domain.repository.UserSessionRepository
import com.example.seedie.domain.repository.VocabularyPracticeRepository
import com.example.seedie.domain.repository.VocabularyQuizRepository
import com.example.seedie.domain.repository.WordBookRepository
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
    abstract fun bindEconomyCloudGateway(
        economyRemoteDataSource: EconomyRemoteDataSource
    ): EconomyCloudGateway

    @Binds
    @Singleton
    abstract fun bindActivityTrackingRepository(
        activityTrackingRepositoryImpl: ActivityTrackingRepositoryImpl
    ): ActivityTrackingRepository

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
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository

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

    @Binds
    @Singleton
    abstract fun bindShopRepository(
        shopRepositoryImpl: ShopRepositoryImpl
    ): ShopRepository

    @Binds
    @Singleton
    abstract fun bindTeacherRepository(
        teacherRepositoryImpl: TeacherRepositoryImpl
    ): TeacherRepository

    @Binds
    @Singleton
    abstract fun bindWordBookRepository(
        wordBookRepositoryImpl: WordBookRepositoryImpl
    ): WordBookRepository
}
