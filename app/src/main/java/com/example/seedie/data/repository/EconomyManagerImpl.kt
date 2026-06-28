package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.data.remote.AuthService
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
    private val authService: AuthService
) : EconomyManager {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val totalTokens: Flow<Int> = authService.currentSession
        .flatMapLatest { session ->
            transactionDao.getTotalTokens(session?.userId ?: "").map { it ?: 0 }
        }

    override suspend fun addTokens(amount: Int, reason: String) {
        if (amount <= 0) return
        val transaction = EconomyTransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = authService.currentSession.value?.userId ?: "",
            timestamp = System.currentTimeMillis(),
            amount = amount,
            reason = reason
        )
        transactionDao.insertTransaction(transaction)
    }

    override suspend fun spendTokens(amount: Int, item: String): Boolean {
        if (amount <= 0) return false
        val userId = authService.currentSession.value?.userId ?: ""
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
            return true
        }
        return false
    }
}
