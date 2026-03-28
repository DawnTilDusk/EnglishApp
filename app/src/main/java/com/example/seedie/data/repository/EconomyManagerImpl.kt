package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.domain.repository.EconomyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EconomyManagerImpl @Inject constructor(
    private val transactionDao: EconomyTransactionDao
) : EconomyManager {

    override val totalTokens: Flow<Int> = transactionDao.getTotalTokens().map { it ?: 0 }

    override suspend fun addTokens(amount: Int, reason: String) {
        if (amount <= 0) return
        val transaction = EconomyTransactionEntity(
            timestamp = System.currentTimeMillis(),
            amount = amount,
            reason = reason
        )
        transactionDao.insertTransaction(transaction)
    }

    override suspend fun spendTokens(amount: Int, item: String): Boolean {
        if (amount <= 0) return false
        val currentTokens = transactionDao.getTotalTokens().first() ?: 0
        if (currentTokens >= amount) {
            val transaction = EconomyTransactionEntity(
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