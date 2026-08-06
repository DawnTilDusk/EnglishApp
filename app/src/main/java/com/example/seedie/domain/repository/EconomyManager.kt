package com.example.seedie.domain.repository

import com.example.seedie.domain.model.BalanceRefreshResult
import kotlinx.coroutines.flow.Flow

interface EconomyManager {
    val totalTokens: Flow<Int>

    suspend fun addTokens(amount: Int, reason: String, refId: String? = null)
    suspend fun spendTokens(amount: Int, item: String, refId: String? = null): Boolean
    suspend fun refreshBalanceFromCloud(): BalanceRefreshResult
}
