package com.example.seedie.data.sync.syncer

import com.example.seedie.data.local.dao.EconomyTransactionDao
import com.example.seedie.data.remote.UserEconomyTransactionDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EconomyTransactionSyncer @Inject constructor(
    private val transactionDao: EconomyTransactionDao,
    private val client: SupabaseClient
) {
    suspend fun sync(userId: String) {
        val pending = transactionDao.getPendingTransactions(userId)
        if (pending.isEmpty()) return

        pending.forEach { tx ->
            try {
                client.postgrest["user_economy_transactions"].upsert(
                    UserEconomyTransactionDto(
                        id = tx.id,
                        user_id = userId,
                        amount = tx.amount,
                        reason = tx.reason,
                        ref_id = null
                    )
                )
                transactionDao.updateSyncStatus(
                    id = tx.id,
                    status = "SYNCED",
                    syncedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                android.util.Log.e("EconomyTransactionSyncer", "Failed to sync tx ${tx.id}", e)
            }
        }
    }
}
