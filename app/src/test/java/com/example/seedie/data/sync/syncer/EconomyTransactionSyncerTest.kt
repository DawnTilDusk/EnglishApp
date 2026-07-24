package com.example.seedie.data.sync.syncer

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.data.remote.EconomyCloudGateway
import com.example.seedie.data.remote.EconomyTransactionEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class EconomyTransactionSyncerTest {

    @Test
    fun sync_deletesOrphansAndDoesNotUploadThem() = runBlocking {
        val dao = FakeEconomyTransactionDao()
        dao.rows["orphan-1"] = EconomyTransactionEntity(
            id = "orphan-1",
            userId = "",
            timestamp = 1L,
            amount = 999,
            reason = "legacy",
            syncStatus = "PENDING"
        )
        val userId = "user-a"
        dao.rows["own-1"] = EconomyTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            timestamp = 2L,
            amount = 10,
            reason = "study",
            syncStatus = "PENDING"
        )

        val remote = FakeEconomyCloudGateway(initialBalance = 0)
        val syncer = EconomyTransactionSyncer(dao, remote)

        val result = syncer.syncAllToCloud(userId)

        assertEquals(null, result.error)
        assertEquals(1, result.syncedCount)
        assertEquals(10, remote.lastSyncedSum)
        assertTrue(dao.rows.keys.none { dao.rows[it]?.userId == "" })
        assertEquals(10, dao.getTotalTokensOnce(userId))
    }

    @Test
    fun sync_onlyUploadsPendingAndMarksSynced() = runBlocking {
        val dao = FakeEconomyTransactionDao()
        val userId = "user-b"
        val pendingId = UUID.randomUUID().toString()
        val syncedId = UUID.randomUUID().toString()
        dao.rows[pendingId] = EconomyTransactionEntity(
            id = pendingId,
            userId = userId,
            timestamp = 1L,
            amount = 5,
            reason = "pending",
            syncStatus = "PENDING"
        )
        dao.rows[syncedId] = EconomyTransactionEntity(
            id = syncedId,
            userId = userId,
            timestamp = 2L,
            amount = 7,
            reason = "already",
            syncStatus = "SYNCED",
            syncedAt = 100L
        )

        val remote = FakeEconomyCloudGateway(initialBalance = 7)
        val syncer = EconomyTransactionSyncer(dao, remote)
        val result = syncer.syncAllToCloud(userId)

        assertEquals(null, result.error)
        assertEquals(1, result.syncedCount)
        assertEquals(listOf(pendingId), remote.lastSyncedIds)
        assertEquals("SYNCED", dao.rows[pendingId]?.syncStatus)
        assertEquals(0, dao.getPendingTokenSumOnce(userId))
    }

    @Test
    fun sync_secondCallUploadsNothingWhenAllSynced() = runBlocking {
        val dao = FakeEconomyTransactionDao()
        val userId = "user-c"
        val id = UUID.randomUUID().toString()
        dao.rows[id] = EconomyTransactionEntity(
            id = id,
            userId = userId,
            timestamp = 1L,
            amount = 3,
            reason = "once",
            syncStatus = "PENDING"
        )
        val remote = FakeEconomyCloudGateway(initialBalance = 0)
        val syncer = EconomyTransactionSyncer(dao, remote)

        syncer.syncAllToCloud(userId)
        remote.syncCallCount = 0
        val second = syncer.syncAllToCloud(userId)

        assertEquals(0, second.syncedCount)
        assertEquals(0, remote.syncCallCount)
        assertEquals(1, remote.fetchCallCount)
    }

    private class FakeEconomyCloudGateway(
        initialBalance: Int
    ) : EconomyCloudGateway {
        var balance: Int = initialBalance
        var lastSyncedSum: Int = 0
        var lastSyncedIds: List<String> = emptyList()
        var syncCallCount: Int = 0
        var fetchCallCount: Int = 0

        override suspend fun fetchMyTokenBalance(): Int {
            fetchCallCount++
            return balance
        }

        override suspend fun syncMyEconomyTransactions(entries: List<EconomyTransactionEntry>): Int {
            syncCallCount++
            lastSyncedIds = entries.map { it.id }
            lastSyncedSum = entries.sumOf { it.amount }
            balance += lastSyncedSum
            return balance
        }
    }

    private class FakeEconomyTransactionDao : EconomyTransactionDao {
        val rows = ConcurrentHashMap<String, EconomyTransactionEntity>()
        private val revision = MutableStateFlow(0)

        private fun bump() {
            revision.value = revision.value + 1
        }

        override fun getTotalTokens(userId: String): Flow<Int?> =
            revision.map { rows.values.filter { it.userId == userId }.sumOf { it.amount } }

        override suspend fun getTotalTokensOnce(userId: String): Int =
            rows.values.filter { it.userId == userId }.sumOf { it.amount }

        override fun getPendingTokenSum(userId: String): Flow<Int?> =
            revision.map {
                rows.values.filter { it.userId == userId && it.syncStatus == "PENDING" }
                    .sumOf { it.amount }
            }

        override suspend fun getPendingTokenSumOnce(userId: String): Int =
            rows.values.filter { it.userId == userId && it.syncStatus == "PENDING" }
                .sumOf { it.amount }

        override fun getAllTransactions(userId: String): Flow<List<EconomyTransactionEntity>> =
            revision.map { rows.values.filter { it.userId == userId }.sortedByDescending { it.timestamp } }

        override suspend fun insertTransaction(transaction: EconomyTransactionEntity): Long {
            if (rows.containsKey(transaction.id)) return -1L
            if (transaction.refId != null &&
                rows.values.any { it.userId == transaction.userId && it.refId == transaction.refId }
            ) {
                return -1L
            }
            rows[transaction.id] = transaction
            bump()
            return 1L
        }

        override suspend fun getPendingTransactions(userId: String): List<EconomyTransactionEntity> =
            rows.values.filter { it.userId == userId && it.syncStatus == "PENDING" }

        override suspend fun getAllTransactionsForUser(userId: String): List<EconomyTransactionEntity> =
            rows.values.filter { it.userId == userId }.sortedBy { it.timestamp }

        override suspend fun findByRefId(userId: String, refId: String): EconomyTransactionEntity? =
            rows.values.firstOrNull { it.userId == userId && it.refId == refId }

        override suspend fun deleteOrphanTransactions(): Int {
            val orphans = rows.filterValues { it.userId == "" }.keys
            orphans.forEach { rows.remove(it) }
            if (orphans.isNotEmpty()) bump()
            return orphans.size
        }

        override suspend fun updateTransactionId(oldId: String, newId: String) {
            val existing = rows.remove(oldId) ?: return
            rows[newId] = existing.copy(id = newId)
            bump()
        }

        override suspend fun updateSyncStatusForIds(ids: List<String>, status: String, syncedAt: Long?) {
            ids.forEach { id ->
                val row = rows[id] ?: return@forEach
                rows[id] = row.copy(syncStatus = status, syncedAt = syncedAt)
            }
            bump()
        }

        override suspend fun updateSyncStatus(id: String, status: String, syncedAt: Long?) {
            updateSyncStatusForIds(listOf(id), status, syncedAt)
        }
    }
}
