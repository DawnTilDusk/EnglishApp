package com.example.seedie.data.remote

interface EconomyCloudGateway {
    suspend fun fetchMyTokenBalance(): Int
    suspend fun syncMyEconomyTransactions(entries: List<EconomyTransactionEntry>): Int
}
