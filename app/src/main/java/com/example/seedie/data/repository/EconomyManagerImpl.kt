package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.EconomyRemoteDataSource
import com.example.seedie.data.sync.syncer.EconomyTransactionSyncer
import com.example.seedie.domain.model.BalanceRefreshResult
import com.example.seedie.domain.repository.EconomyManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EconomyManagerImpl @Inject constructor(
    private val transactionDao: EconomyTransactionDao,
    private val authService: AuthService,
    private val economyTransactionSyncer: EconomyTransactionSyncer,
    private val economyRemote: EconomyRemoteDataSource
) : EconomyManager {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val totalTokens: Flow<Int> = authService.currentSession
        .flatMapLatest { session ->
            transactionDao.getTotalTokens(session?.userId ?: "").map { it ?: 0 }
        }

    override suspend fun addTokens(amount: Int, reason: String) {
        if (amount <= 0) return
        val userId = authService.currentSession.value?.userId ?: return
        val transaction = EconomyTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            timestamp = System.currentTimeMillis(),
            amount = amount,
            reason = reason
        )
        transactionDao.insertTransaction(transaction)
        val result = economyTransactionSyncer.syncAllToCloud(userId)
        if (result.error != null) {
            android.util.Log.w(
                "EconomyManagerImpl",
                "Token sync failed for user $userId: ${result.error}"
            )
        }
    }

    override suspend fun spendTokens(amount: Int, item: String): Boolean {
        if (amount <= 0) return false
        val userId = authService.currentSession.value?.userId ?: return false
        val currentTokens = transactionDao.getTotalTokens(userId).first() ?: 0
        if (currentTokens >= amount) {
            val transaction = EconomyTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                timestamp = System.currentTimeMillis(),
                amount = -amount,
                reason = "Bought: $item"
            )
            transactionDao.insertTransaction(transaction)
            economyTransactionSyncer.syncAllToCloud(userId)
            return true
        }
        return false
    }

    override suspend fun refreshBalanceFromCloud(): BalanceRefreshResult {
        return try {
            val userId = authService.currentSession.value?.userId
                ?: return BalanceRefreshResult(
                    cloudBalance = 0,
                    localBalance = 0,
                    success = false,
                    errorMessage = "Not logged in"
                )

            val syncResult = economyTransactionSyncer.syncAllToCloud(userId)
            if (syncResult.error != null) {
                return BalanceRefreshResult(
                    cloudBalance = syncResult.cloudBalance,
                    localBalance = syncResult.localSum,
                    success = false,
                    errorMessage = syncResult.error
                )
            }

            var cloudBalance = syncResult.cloudBalance
            var localBalance = transactionDao.getTotalTokens(userId).first() ?: syncResult.localSum

            if (cloudBalance > localBalance) {
                val diff = cloudBalance - localBalance
                transactionDao.insertTransaction(
                    EconomyTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        timestamp = System.currentTimeMillis(),
                        amount = diff,
                        reason = "Cloud balance sync",
                        syncStatus = "SYNCED",
                        syncedAt = System.currentTimeMillis()
                    )
                )
                localBalance = transactionDao.getTotalTokens(userId).first() ?: localBalance
            }

            if (cloudBalance < localBalance) {
                return BalanceRefreshResult(
                    cloudBalance = cloudBalance,
                    localBalance = localBalance,
                    success = false,
                    errorMessage = "代币同步未完成：本地 $localBalance，云端 $cloudBalance。请联网完成学习同步后再试。"
                )
            }

            economyTransactionSyncer.markAllSyncedForUser(userId)
            BalanceRefreshResult(
                cloudBalance = cloudBalance,
                localBalance = localBalance,
                success = true
            )
        } catch (e: Exception) {
            BalanceRefreshResult(
                cloudBalance = 0,
                localBalance = 0,
                success = false,
                errorMessage = e.message ?: e.toString()
            )
        }
    }
}
