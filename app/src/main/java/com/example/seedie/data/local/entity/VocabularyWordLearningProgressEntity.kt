package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "vocabulary_word_learning_progress",
    primaryKeys = ["userId", "bookId", "wordId"],
    indices = [
        Index(value = ["userId", "bookId"]),
        Index(value = ["userId", "bookId", "status"])
    ]
)
data class VocabularyWordLearningProgressEntity(
    val userId: String,
    val bookId: String,
    val wordId: String,
    val status: String,
    val lastStudiedAt: Long,
    val learnedAt: Long?,
    val syncStatus: String = "PENDING",
    val syncedAt: Long? = null
)
