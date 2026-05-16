package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary_book_progress")
data class VocabularyBookProgressEntity(
    @PrimaryKey
    val bookId: String,
    val nextWordSortOrderCursor: Int,
    val activeRoundId: String?,
    val learnedWordCount: Int,
    val updatedAt: Long
)
