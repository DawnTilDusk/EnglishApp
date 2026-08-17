package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.remote.EconomyCloudGateway
import com.example.seedie.data.sync.syncer.EconomyTransactionSyncer
import com.example.seedie.di.ApplicationScope
import com.example.seedie.domain.model.BalanceRefreshResult
import com.example.seedie.domain.model.projectDisplayedTokenBalance
import com.example.seedie.domain.model.shouldSkipDuplicateTokenGrant
import com.example.seedie.domain.repository.EconomyManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Singleton
class EconomyManagerImpl @Inject constructor(
    private val transactionDao: EconomyTransactionDao,
    private val authService: AuthService,
    private val economyTransactionSyncer: EconomyTransactionSyncer,
    private val economyRemote: EconomyCloudGateway,
    @param:ApplicationScope private val appScope: CoroutineScope
) : EconomyManager {

    /** Per-user last successfully observed cloud balance; null means cold start (use local sum). */
    private val cloudBalanceCache = ConcurrentHashMap<String, Int>()
    private val cloudCacheRevision = MutableStateFlow(0)

    init {
        // Profile / garden / picker only collect totalTokens — they never called
        // refreshBalanceFromCloud. Without hydrating on login, a wiped local DB
        // shows 0 forever even when cloud ledger has a large balance.
        appScope.launch {
            authService.currentSession
                .map { it?.userId?.takeIf { id -> id.isNotBlank() } }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId == null) {
                        cloudBalanceCache.clear()
                        cloudCacheRevision.value = cloudCacheRevision.value + 1
                    } else {
                        hydrateCloudBalance(userId)
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val totalTokens: Flow<Int> = authService.currentSession
        .flatMapLatest { session ->
            val userId = session?.userId
            if (userId.isNullOrBlank()) {
                flowOf(0)
            } else {
                combine(
                    transactionDao.getTotalTokens(userId).map { it ?: 0 },
                    transactionDao.getPendingTokenSum(userId).map { it ?: 0 },
                    cloudCacheRevision
                ) { localSum, pendingSum, _ ->
                    projectDisplayedTokenBalance(
                        cloudBalance = cloudBalanceCache[userId],
                        localLedgerSum = localSum,
                        pendingSum = pendingSum
                    )
                }
            }
        }

    override suspend fun addTokens(amount: Int, reason: String, refId: String?) {
        if (amount <= 0) return
        val userId = authService.currentSession.value?.userId ?: return
        val existing = if (!refId.isNullOrBlank()) {
            transactionDao.findByRefId(userId, refId) != null
        } else {
            false
        }
        if (shouldSkipDuplicateTokenGrant(refId, existing)) return

        val transaction = EconomyTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            timestamp = System.currentTimeMillis(),
            amount = amount,
            reason = reason,
            refId = refId?.takeIf { it.isNotBlank() }
        )
        val inserted = transactionDao.insertTransaction(transaction)
        if (inserted == -1L) return
        val result = economyTransactionSyncer.syncAllToCloud(userId)
        applySyncBalance(userId, result.cloudBalance, result.error)
    }

    override suspend fun spendTokens(amount: Int, item: String, refId: String?): Boolean {
        if (amount <= 0) return false
        val userId = authService.currentSession.value?.userId ?: return false
        if (!refId.isNullOrBlank() && transactionDao.findByRefId(userId, refId) != null) {
            return true
        }
        if (projectedBalance(userId) < amount) return false

        val transaction = EconomyTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            timestamp = System.currentTimeMillis(),
            amount = -amount,
            reason = "Bought: $item",
            refId = refId?.takeIf { it.isNotBlank() }
        )
        val inserted = transactionDao.insertTransaction(transaction)
        if (inserted == -1L) {
            return !refId.isNullOrBlank() && transactionDao.findByRefId(userId, refId) != null
        }
        val result = economyTransactionSyncer.syncAllToCloud(userId)
        applySyncBalance(userId, result.cloudBalance, result.error)
        return true
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
            val pendingSum = transactionDao.getPendingTokenSumOnce(userId)
            val localLedgerSum = transactionDao.getTotalTokensOnce(userId)

            val cloudBalance = runCatching { economyRemote.fetchMyTokenBalance() }
                .getOrDefault(syncResult.cloudBalance)
            // Always cache a successful cloud read for UI, even if PENDING upload failed.
            rememberCloudBalance(userId, cloudBalance)

            val projected = projectDisplayedTokenBalance(
                cloudBalance = cloudBalance,
                localLedgerSum = localLedgerSum,
                pendingSum = pendingSum
            )

            if (syncResult.error != null) {
                return BalanceRefreshResult(
                    cloudBalance = cloudBalance,
                    localBalance = projected,
                    success = false,
                    errorMessage = syncResult.error
                )
            }

            if (localLedgerSum > cloudBalance && pendingSum == 0) {
                return BalanceRefreshResult(
                    cloudBalance = cloudBalance,
                    localBalance = localLedgerSum,
                    success = false,
                    errorMessage = "代币同步未完成：本地 $localLedgerSum，云端 $cloudBalance。请联网完成学习同步后再试。"
                )
            }

            BalanceRefreshResult(
                cloudBalance = cloudBalance,
                localBalance = projected,
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

    private suspend fun hydrateCloudBalance(userId: String) {
        runCatching {
            val syncResult = economyTransactionSyncer.syncAllToCloud(userId)
            val cloudBalance = runCatching { economyRemote.fetchMyTokenBalance() }
                .getOrDefault(syncResult.cloudBalance)
            rememberCloudBalance(userId, cloudBalance)
        }
    }

    /**
     * Prefer an explicit cloud read after sync; if upload failed but syncer still
     * returned a balance (or we can fetch), keep UI aligned with cloud.
     */
    private suspend fun applySyncBalance(userId: String, syncCloudBalance: Int, error: String?) {
        if (error == null) {
            rememberCloudBalance(userId, syncCloudBalance)
            return
        }
        val fetched = runCatching { economyRemote.fetchMyTokenBalance() }.getOrNull()
        if (fetched != null) {
            rememberCloudBalance(userId, fetched)
        } else if (syncCloudBalance != 0 || cloudBalanceCache[userId] == null) {
            // Keep a non-zero syncer reading; avoid overwriting a good cache with 0 on failure.
            if (syncCloudBalance != 0) {
                rememberCloudBalance(userId, syncCloudBalance)
            }
        }
    }

    private suspend fun projectedBalance(userId: String): Int {
        return projectDisplayedTokenBalance(
            cloudBalance = cloudBalanceCache[userId],
            localLedgerSum = transactionDao.getTotalTokensOnce(userId),
            pendingSum = transactionDao.getPendingTokenSumOnce(userId)
        )
    }

    private fun rememberCloudBalance(userId: String, balance: Int) {
        cloudBalanceCache[userId] = balance
        cloudCacheRevision.value = cloudCacheRevision.value + 1
    }
}
