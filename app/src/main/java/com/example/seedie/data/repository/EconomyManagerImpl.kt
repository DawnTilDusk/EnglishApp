package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import com.example.seedie.di.IoDispatcher
import com.example.seedie.domain.repository.EconomyManager
import com.example.seedie.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EconomyManagerImpl @Inject constructor(
    private val transactionDao: EconomyTransactionDao,
    private val userSessionRepository: UserSessionRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : EconomyManager {

    override val totalTokens: Flow<Int?> = transactionDao.getTotalTokens()
    override val transactions: Flow<List<EconomyTransactionEntity>> = transactionDao.getAllTransactions()

    override suspend fun addTokens(amount: Int, reason: String) {
        require(amount > 0) { "Amount must be positive" }
        withContext(ioDispatcher) {
            val transaction = EconomyTransactionEntity(
                timestamp = System.currentTimeMillis(),
                amount = amount,
                reason = reason
            )
            transactionDao.insertTransaction(transaction)
            userSessionRepository.updateTokens(amount)
        }
    }

    override suspend fun spendTokens(amount: Int, reason: String): Boolean {
        require(amount > 0) { "Amount must be positive" }
        return withContext(ioDispatcher) {
            val currentTotal = totalTokens.firstOrNull() ?: 0
            if (currentTotal >= amount) {
                val transaction = EconomyTransactionEntity(
                    timestamp = System.currentTimeMillis(),
                    amount = -amount,
                    reason = reason
                )
                transactionDao.insertTransaction(transaction)
                userSessionRepository.updateTokens(-amount)
                true
            } else {
                false
            }
        }
    }
}