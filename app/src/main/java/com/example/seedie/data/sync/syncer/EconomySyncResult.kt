package com.example.seedie.data.sync.syncer

data class EconomySyncResult(
    val cloudBalance: Int,
    val localSum: Int,
    val syncedCount: Int,
    val error: String? = null
)
