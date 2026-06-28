package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.VocabularyWordEntity

@Dao
interface VocabularyWordDao {
    @Query("SELECT * FROM vocabulary_words WHERE bookId = :bookId ORDER BY sortOrder ASC")
    suspend fun getWordsByBook(bookId: String): List<VocabularyWordEntity>

    @Query(
        """
        SELECT * FROM vocabulary_words
        WHERE bookId = :bookId AND sortOrder >= :startSortOrder
        ORDER BY sortOrder ASC
        LIMIT :limit
        """
    )
    suspend fun getWordsByBookFromSortOrder(
        bookId: String,
        startSortOrder: Int,
        limit: Int
    ): List<VocabularyWordEntity>

    @Query("SELECT * FROM vocabulary_words WHERE bookId = :bookId AND wordId IN (:wordIds)")
    suspend fun getWordsByIds(
        bookId: String,
        wordIds: List<String>
    ): List<VocabularyWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<VocabularyWordEntity>)

    @Query("DELETE FROM vocabulary_words WHERE bookId = :bookId")
    suspend fun deleteWordsByBook(bookId: String)

    @Query("SELECT COUNT(*) FROM vocabulary_words WHERE bookId = :bookId")
    suspend fun getWordCountByBook(bookId: String): Int
}
