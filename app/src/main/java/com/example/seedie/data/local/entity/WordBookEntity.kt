package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "word_books")
data class WordBookEntity(
    @PrimaryKey
    val bookId: String,
    val title: String,
    val description: String,
    val language: String,
    val difficulty: String,
    val version: Int,
    val sourceType: String,
    val downloadStatus: String,
    val isActive: Boolean,
    val wordCount: Int,
    val updatedAt: Long,
    val coverUrl: String? = null,
    /** Numeric grade-based difficulty (1 = 一年级上，18 = 九年级下，可继续扩展)。可空。 */
    val gradeLevel: Int? = null
)
