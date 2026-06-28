package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.WordBookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordBookDao {

    @Query("SELECT COUNT(*) FROM word_books")
    suspend fun getBookCount(): Int

    @Query("SELECT * FROM word_books WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveBook(): WordBookEntity?

    @Query("SELECT * FROM word_books ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestBook(): WordBookEntity?

    @Query("SELECT * FROM word_books ORDER BY title ASC")
    fun getAllBooks(): Flow<List<WordBookEntity>>

    @Query("SELECT * FROM word_books ORDER BY title ASC")
    suspend fun getAllBooksOnce(): List<WordBookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: WordBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<WordBookEntity>)

    @Query("UPDATE word_books SET downloadStatus = :status WHERE bookId = :bookId")
    suspend fun updateDownloadStatus(bookId: String, status: String)

    @Query("UPDATE word_books SET isActive = 0")
    suspend fun deactivateAllBooks()

    @Query("UPDATE word_books SET isActive = 1 WHERE bookId = :bookId")
    suspend fun setActiveBook(bookId: String)

    @Query("SELECT * FROM word_books WHERE bookId = :bookId")
    suspend fun getBookById(bookId: String): WordBookEntity?
}
