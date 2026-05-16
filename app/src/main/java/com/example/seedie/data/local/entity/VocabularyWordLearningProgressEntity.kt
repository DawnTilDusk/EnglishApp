package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "vocabulary_word_learning_progress",
    primaryKeys = ["bookId", "wordId"],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "status"])
    ]
)
data class VocabularyWordLearningProgressEntity(
    val bookId: String,
    val wordId: String,
    val status: String,
    val lastStudiedAt: Long,
    val learnedAt: Long?
)
