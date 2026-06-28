package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "vocabulary_book_progress",
    primaryKeys = ["userId", "bookId"],
    indices = [Index(value = ["activeRoundId"])]
)
data class VocabularyBookProgressEntity(
    val userId: String,
    val bookId: String,
    val nextWordSortOrderCursor: Int,
    val activeRoundId: String?,
    val learnedWordCount: Int,
    val updatedAt: Long,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
)
