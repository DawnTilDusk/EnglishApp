package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.UserEconomyTransactionDto
import com.example.seedie.data.sync.SyncManager
import com.example.seedie.data.sync.SyncScope
import com.example.seedie.domain.repository.EconomyManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
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
    private val syncManager: SyncManager,
    private val client: SupabaseClient
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
        syncManager.syncNow(SyncScope.ECONOMY)
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
            syncManager.syncNow(SyncScope.ECONOMY)
            return true
        }
        return false
    }

    override suspend fun refreshBalanceFromCloud(): Result<Int> {
        return try {
            val userId = authService.currentSession.value?.userId
                ?: return Result.failure(IllegalStateException("Not logged in"))

            syncManager.syncNow(SyncScope.ECONOMY)

            val cloudRows = client.postgrest["user_economy_transactions"].select {
                filter { eq("user_id", userId) }
            }.decodeList<UserEconomyTransactionDto>()

            val cloudBalance = cloudRows.sumOf { it.amount }
            val localBalance = transactionDao.getTotalTokens(userId).first() ?: 0

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
            }

            Result.success(cloudBalance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
