package com.example.seedie.data.sync.syncer

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.remote.EconomyCloudGateway
import com.example.seedie.data.remote.EconomyTransactionEntry
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EconomyTransactionSyncer @Inject constructor(
    private val transactionDao: EconomyTransactionDao,
    private val economyRemote: EconomyCloudGateway
) {
    private val standardUuidRegex = Regex(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    )

    suspend fun sync(userId: String) {
        syncAllToCloud(userId)
    }

    suspend fun syncAllToCloud(userId: String): EconomySyncResult {
        return try {
            transactionDao.deleteOrphanTransactions()
            repairNonStandardPendingIds(userId)

            val pending = transactionDao.getPendingTransactions(userId)
            val localSum = transactionDao.getTotalTokensOnce(userId)

            if (pending.isEmpty()) {
                val balance = economyRemote.fetchMyTokenBalance()
                return EconomySyncResult(
                    cloudBalance = balance,
                    localSum = localSum,
                    syncedCount = 0
                )
            }

            val entries = pending.map { tx ->
                EconomyTransactionEntry(
                    id = tx.id,
                    amount = tx.amount,
                    reason = tx.reason,
                    refId = tx.refId
                )
            }
            val cloudBalance = economyRemote.syncMyEconomyTransactions(entries)
            transactionDao.updateSyncStatusForIds(
                ids = pending.map { it.id },
                status = "SYNCED",
                syncedAt = System.currentTimeMillis()
            )
            EconomySyncResult(
                cloudBalance = cloudBalance,
                localSum = localSum,
                syncedCount = pending.size
            )
        } catch (e: Exception) {
            val localSum = runCatching { transactionDao.getTotalTokensOnce(userId) }.getOrDefault(0)
            val cloudBalance = runCatching { economyRemote.fetchMyTokenBalance() }.getOrDefault(0)
            EconomySyncResult(
                cloudBalance = cloudBalance,
                localSum = localSum,
                syncedCount = 0,
                error = e.message ?: e.toString()
            )
        }
    }

    private suspend fun repairNonStandardPendingIds(userId: String) {
        val pending = transactionDao.getPendingTransactions(userId)
        pending.forEach { tx ->
            if (!standardUuidRegex.matches(tx.id)) {
                transactionDao.updateTransactionId(tx.id, UUID.randomUUID().toString())
            }
        }
    }
}
