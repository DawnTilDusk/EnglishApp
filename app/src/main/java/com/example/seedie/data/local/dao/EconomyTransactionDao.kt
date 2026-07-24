package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EconomyTransactionDao {
    @Query("SELECT SUM(amount) FROM economy_transactions WHERE userId = :userId")
    fun getTotalTokens(userId: String): Flow<Int?>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM economy_transactions WHERE userId = :userId")
    suspend fun getTotalTokensOnce(userId: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM economy_transactions
        WHERE userId = :userId AND syncStatus = 'PENDING'
        """
    )
    fun getPendingTokenSum(userId: String): Flow<Int?>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM economy_transactions
        WHERE userId = :userId AND syncStatus = 'PENDING'
        """
    )
    suspend fun getPendingTokenSumOnce(userId: String): Int

    @Query("SELECT * FROM economy_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllTransactions(userId: String): Flow<List<EconomyTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: EconomyTransactionEntity): Long

    @Query("SELECT * FROM economy_transactions WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingTransactions(userId: String): List<EconomyTransactionEntity>

    @Query("SELECT * FROM economy_transactions WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getAllTransactionsForUser(userId: String): List<EconomyTransactionEntity>

    @Query(
        """
        SELECT * FROM economy_transactions
        WHERE userId = :userId AND refId = :refId
        LIMIT 1
        """
    )
    suspend fun findByRefId(userId: String, refId: String): EconomyTransactionEntity?

    @Query("DELETE FROM economy_transactions WHERE userId = ''")
    suspend fun deleteOrphanTransactions(): Int

    @Query("UPDATE economy_transactions SET id = :newId WHERE id = :oldId")
    suspend fun updateTransactionId(oldId: String, newId: String)

    @Query(
        """
        UPDATE economy_transactions
        SET syncStatus = :status, syncedAt = :syncedAt
        WHERE id IN (:ids)
        """
    )
    suspend fun updateSyncStatusForIds(ids: List<String>, status: String, syncedAt: Long?)

    @Query("UPDATE economy_transactions SET syncStatus = :status, syncedAt = :syncedAt WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, syncedAt: Long?)
}
