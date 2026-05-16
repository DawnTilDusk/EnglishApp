package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary_study_rounds",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "status"])
    ]
)
data class VocabularyStudyRoundEntity(
    @PrimaryKey
    val roundId: String,
    val bookId: String,
    val status: String,
    val targetWordCount: Int,
    val introducedWordCount: Int,
    val masteredWordCount: Int,
    val activeQueueSize: Int,
    val nextWordSortOrderCursor: Int,
    val createdAt: Long,
    val updatedAt: Long
)
