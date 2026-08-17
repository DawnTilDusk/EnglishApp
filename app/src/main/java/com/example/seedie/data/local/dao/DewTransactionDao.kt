package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.DewTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DewTransactionDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM dew_transactions WHERE userId = :userId")
    fun getTotalDews(userId: String): Flow<Int?>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM dew_transactions WHERE userId = :userId")
    suspend fun getTotalDewsOnce(userId: String): Int

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM dew_transactions
        WHERE userId = :userId AND syncStatus = 'PENDING'
        """
    )
    fun getPendingDewSum(userId: String): Flow<Int?>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM dew_transactions
        WHERE userId = :userId AND syncStatus = 'PENDING'
        """
    )
    suspend fun getPendingDewSumOnce(userId: String): Int

    @Query("SELECT * FROM dew_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllTransactions(userId: String): Flow<List<DewTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: DewTransactionEntity): Long

    @Query("SELECT * FROM dew_transactions WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingTransactions(userId: String): List<DewTransactionEntity>

    @Query(
        """
        SELECT * FROM dew_transactions
        WHERE userId = :userId AND refId = :refId
        LIMIT 1
        """
    )
    suspend fun findByRefId(userId: String, refId: String): DewTransactionEntity?

    @Query("DELETE FROM dew_transactions WHERE userId = ''")
    suspend fun deleteOrphanTransactions(): Int

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM dew_transactions
        WHERE userId = :userId AND amount > 0 AND timestamp >= :dayStartMillis AND timestamp < :dayEndMillis
        """
    )
    suspend fun getTodayDewIncomeSum(
        userId: String,
        dayStartMillis: Long,
        dayEndMillis: Long
    ): Int

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM dew_transactions
        WHERE userId = :userId
          AND reason LIKE 'Convert: %' AND amount > 0
          AND timestamp >= :dayStartMillis AND timestamp < :dayEndMillis
        """
    )
    suspend fun getTodayConvertedDewSum(
        userId: String,
        dayStartMillis: Long,
        dayEndMillis: Long
    ): Int
}
