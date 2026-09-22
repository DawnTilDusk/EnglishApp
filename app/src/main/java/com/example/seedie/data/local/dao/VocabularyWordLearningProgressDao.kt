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

    /**
     * 取当前用户在某本书里处于给定 status 的所有 wordId，按 lastStudiedAt 升序（学得最久没碰的优先）。
     * 用于把"打回 LEARNING 后被 cursor 跳过的老词"重新塞回新的学习轮次。
     */
    @Query(
        "SELECT wordId FROM vocabulary_word_learning_progress WHERE userId = :userId AND bookId = :bookId AND status = :status ORDER BY lastStudiedAt ASC"
    )
    suspend fun getWordIdsByStatus(userId: String, bookId: String, status: String): List<String>

    @Query("SELECT wordId FROM vocabulary_word_learning_progress WHERE userId = :userId AND bookId = :bookId AND status IN (:statuses) ORDER BY lastStudiedAt ASC, wordId ASC")
    suspend fun getWordIdsByStatuses(userId: String, bookId: String, statuses: List<String>): List<String>

    @Query("SELECT COUNT(*) FROM vocabulary_word_learning_progress WHERE userId = :userId AND bookId = :bookId AND status IN (:statuses)")
    suspend fun countWordsInStatuses(userId: String, bookId: String, statuses: List<String>): Int
}
