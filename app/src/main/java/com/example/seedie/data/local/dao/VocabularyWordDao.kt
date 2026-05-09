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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<VocabularyWordEntity>)
}
