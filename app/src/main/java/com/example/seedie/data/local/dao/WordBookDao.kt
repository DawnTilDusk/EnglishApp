package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.WordBookEntity

@Dao
interface WordBookDao {
    @Query("SELECT COUNT(*) FROM word_books")
    suspend fun getBookCount(): Int

    @Query("SELECT * FROM word_books WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveBook(): WordBookEntity?

    @Query("SELECT * FROM word_books ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestBook(): WordBookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: WordBookEntity)
}
