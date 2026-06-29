package com.example.seedie.domain.model

data class BalanceRefreshResult(
    val cloudBalance: Int,
    val localBalance: Int,
    val success: Boolean,
    val errorMessage: String? = null
)
