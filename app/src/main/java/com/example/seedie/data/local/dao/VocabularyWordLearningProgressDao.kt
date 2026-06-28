package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.VocabularyWordLearningProgressEntity

@Dao
interface VocabularyWordLearningProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(progressList: List<VocabularyWordLearningProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(progress: VocabularyWordLearningProgressEntity)

    @Query("SELECT * FROM vocabulary_word_learning_progress WHERE userId = :userId AND bookId = :bookId AND wordId IN (:wordIds)")
    suspend fun getProgressByWordIds(
        userId: String,
        bookId: String,
        wordIds: List<String>
    ): List<VocabularyWordLearningProgressEntity>

    @Query("SELECT * FROM vocabulary_word_learning_progress WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingProgress(userId: String): List<VocabularyWordLearningProgressEntity>

    @Query("UPDATE vocabulary_word_learning_progress SET syncStatus = :status, syncedAt = :syncedAt WHERE userId = :userId AND bookId = :bookId AND wordId = :wordId")
    suspend fun updateSyncStatus(userId: String, bookId: String, wordId: String, status: String, syncedAt: Long?)
}
