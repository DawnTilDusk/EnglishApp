package com.example.seedie.data.sync

import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.sync.syncer.CheckInSyncer
import com.example.seedie.data.sync.syncer.EconomyTransactionSyncer
import com.example.seedie.data.sync.syncer.VocabularyProgressSyncer
import com.example.seedie.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManagerImpl @Inject constructor(
    private val checkInSyncer: CheckInSyncer,
    private val vocabularyProgressSyncer: VocabularyProgressSyncer,
    private val economyTransactionSyncer: EconomyTransactionSyncer,
    private val networkObserver: NetworkConnectivityObserver,
    private val authService: AuthService,
    @ApplicationScope private val appScope: CoroutineScope
) : SyncManager {

    init {
        appScope.launch {
            networkObserver.observe()
                .filter { it }
                .collect { syncNow() }
        }
    }

    override suspend fun syncNow(scope: SyncScope): SyncResult {
        val userId = authService.currentSession.value?.userId
            ?: return SyncResult.Failure("Not logged in")
        return try {
            when (scope) {
                SyncScope.ALL -> {
                    checkInSyncer.sync(userId)
                    vocabularyProgressSyncer.sync(userId)
                    economyTransactionSyncer.sync(userId)
                }
                SyncScope.CHECK_IN -> checkInSyncer.sync(userId)
                SyncScope.VOCABULARY_PROGRESS -> vocabularyProgressSyncer.sync(userId)
                SyncScope.ECONOMY -> economyTransactionSyncer.sync(userId)
            }
            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Failure(e.message ?: "Unknown sync error")
        }
    }
}
