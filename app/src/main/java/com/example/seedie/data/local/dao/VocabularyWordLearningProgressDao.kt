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

    @Query("SELECT * FROM vocabulary_word_learning_progress WHERE bookId = :bookId AND wordId IN (:wordIds)")
    suspend fun getProgressByWordIds(
        bookId: String,
        wordIds: List<String>
    ): List<VocabularyWordLearningProgressEntity>
}
