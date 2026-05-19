package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EconomyTransactionDao {
    @Query("SELECT SUM(amount) FROM economy_transactions WHERE userId = :userId")
    fun getTotalTokens(userId: String): Flow<Int?>

    @Query("SELECT * FROM economy_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllTransactions(userId: String): Flow<List<EconomyTransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: EconomyTransactionEntity)
}