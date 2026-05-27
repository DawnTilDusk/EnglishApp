package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.WordBookModuleEntity

@Dao
interface WordBookModuleDao {

    @Query("SELECT * FROM word_book_modules WHERE bookId = :bookId ORDER BY sortOrder ASC")
    suspend fun getModulesByBook(bookId: String): List<WordBookModuleEntity>

    @Query("SELECT * FROM word_book_modules WHERE moduleId = :moduleId")
    suspend fun getModuleById(moduleId: String): WordBookModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<WordBookModuleEntity>)

    @Query("DELETE FROM word_book_modules WHERE bookId = :bookId")
    suspend fun deleteModulesByBook(bookId: String)
}
