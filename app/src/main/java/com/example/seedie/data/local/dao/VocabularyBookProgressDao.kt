package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.VocabularyBookProgressEntity

@Dao
interface VocabularyBookProgressDao {
    @Query("SELECT * FROM vocabulary_book_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun getProgressByBookId(bookId: String): VocabularyBookProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(progress: VocabularyBookProgressEntity)
}
