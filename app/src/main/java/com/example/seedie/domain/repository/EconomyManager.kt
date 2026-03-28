package com.example.seedie.domain.repository

import kotlinx.coroutines.flow.Flow

interface EconomyManager {
    val totalTokens: Flow<Int>

    suspend fun addTokens(amount: Int, reason: String)
    suspend fun spendTokens(amount: Int, item: String): Boolean
}