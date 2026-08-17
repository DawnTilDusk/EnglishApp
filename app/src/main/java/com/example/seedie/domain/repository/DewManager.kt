package com.example.seedie.domain.repository

import com.example.seedie.domain.model.ConvertTokensToDewsResult
import kotlinx.coroutines.flow.Flow

interface DewManager {
    val totalDews: Flow<Int>

    suspend fun addDews(
        amount: Int,
        reason: String,
        refId: String? = null,
        respectCap: Boolean = true
    )

    suspend fun spendDews(
        amount: Int,
        item: String,
        refId: String? = null
    ): Boolean

    suspend fun convertTokensToDews(tokenAmount: Int): ConvertTokensToDewsResult
}
