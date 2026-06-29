package com.example.seedie.data.sync

interface SyncManager {
    suspend fun syncNow(scope: SyncScope = SyncScope.ALL): SyncResult
}

enum class SyncScope { ALL, CHECK_IN, VOCABULARY_PROGRESS, ECONOMY }

sealed class SyncResult {
    object Success : SyncResult()
    data class Failure(val message: String) : SyncResult()
}
