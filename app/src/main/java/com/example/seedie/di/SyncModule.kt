package com.example.seedie.di

import com.example.seedie.data.sync.NetworkConnectivityObserver
import com.example.seedie.data.sync.NetworkConnectivityObserverImpl
import com.example.seedie.data.sync.SyncManager
import com.example.seedie.data.sync.SyncManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSyncManager(impl: SyncManagerImpl): SyncManager

    @Binds
    @Singleton
    abstract fun bindNetworkObserver(impl: NetworkConnectivityObserverImpl): NetworkConnectivityObserver
}
