package com.example.seedie.data.sync.syncer

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.remote.EconomyRemoteDataSource
import com.example.seedie.data.remote.EconomyTransactionEntry
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EconomyTransactionSyncer @Inject constructor(
    private val transactionDao: EconomyTransactionDao,
    private val economyRemote: EconomyRemoteDataSource
) {
    private val standardUuidRegex = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    )

    suspend fun sync(userId: String) {
        syncAllToCloud(userId)
    }

    suspend fun syncAllToCloud(userId: String): EconomySyncResult {
        return try {
            transactionDao.claimOrphanTransactions(userId)
            repairNonStandardTransactionIds(userId)

            val all = transactionDao.getAllTransactionsForUser(userId)
            val localSum = all.sumOf { it.amount }

            if (all.isEmpty()) {
                val balance = economyRemote.fetchMyTokenBalance()
                return EconomySyncResult(
                    cloudBalance = balance,
                    localSum = 0,
                    syncedCount = 0
                )
            }

            val entries = all.map { tx ->
                EconomyTransactionEntry(
                    id = tx.id,
                    amount = tx.amount,
                    reason = tx.reason
                )
            }
            val cloudBalance = economyRemote.syncMyEconomyTransactions(entries)
            EconomySyncResult(
                cloudBalance = cloudBalance,
                localSum = localSum,
                syncedCount = all.size
            )
        } catch (e: Exception) {
            android.util.Log.e("EconomyTransactionSyncer", "Batch sync failed", e)
            val all = runCatching { transactionDao.getAllTransactionsForUser(userId) }.getOrDefault(emptyList())
            val localSum = all.sumOf { it.amount }
            val cloudBalance = runCatching { economyRemote.fetchMyTokenBalance() }.getOrDefault(0)
            EconomySyncResult(
                cloudBalance = cloudBalance,
                localSum = localSum,
                syncedCount = 0,
                error = e.message ?: e.toString()
            )
        }
    }

    suspend fun markAllSyncedForUser(userId: String) {
        transactionDao.markAllSyncedForUser(userId, System.currentTimeMillis())
    }

    private suspend fun repairNonStandardTransactionIds(userId: String) {
        val all = transactionDao.getAllTransactionsForUser(userId)
        all.forEach { tx ->
            if (!standardUuidRegex.matches(tx.id)) {
                transactionDao.updateTransactionId(tx.id, UUID.randomUUID().toString())
            }
        }
    }
}
