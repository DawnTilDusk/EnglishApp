package com.example.seedie.domain.repository

import com.example.seedie.data.local.entity.EconomyTransactionEntity
import kotlinx.coroutines.flow.Flow

interface EconomyManager {
    val totalTokens: Flow<Int?>
    val transactions: Flow<List<EconomyTransactionEntity>>

    suspend fun addTokens(amount: Int, reason: String)
    suspend fun spendTokens(amount: Int, reason: String): Boolean
}