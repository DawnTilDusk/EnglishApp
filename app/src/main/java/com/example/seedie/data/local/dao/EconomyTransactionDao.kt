package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.seedie.data.local.entity.EconomyTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EconomyTransactionDao {
    @Query("SELECT SUM(amount) FROM economy_transactions")
    fun getTotalTokens(): Flow<Int?>

    @Query("SELECT * FROM economy_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<EconomyTransactionEntity>>

    @Insert
    suspend fun insertTransaction(transaction: EconomyTransactionEntity)
}