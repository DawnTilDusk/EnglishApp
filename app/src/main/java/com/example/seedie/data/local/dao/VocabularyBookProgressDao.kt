package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.VocabularyBookProgressEntity

@Dao
interface VocabularyBookProgressDao {
    @Query("SELECT * FROM vocabulary_book_progress WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getProgressByBookId(userId: String, bookId: String): VocabularyBookProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(progress: VocabularyBookProgressEntity)

    @Query("SELECT * FROM vocabulary_book_progress WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingProgress(userId: String): List<VocabularyBookProgressEntity>

    @Query("UPDATE vocabulary_book_progress SET syncStatus = :status, syncedAt = :syncedAt WHERE userId = :userId AND bookId = :bookId")
    suspend fun updateSyncStatus(userId: String, bookId: String, status: String, syncedAt: Long?)
}
