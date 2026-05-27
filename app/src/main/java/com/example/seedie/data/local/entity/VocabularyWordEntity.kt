package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary_words",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "difficultyLevel"]),
        Index(value = ["moduleId"])
    ]
)
data class VocabularyWordEntity(
    @PrimaryKey
    val wordId: String,
    val bookId: String,
    val english: String,
    val phonetic: String,
    val partOfSpeech: String,
    val translation: String,
    val exampleSentence: String,
    val difficultyLevel: String,
    val rewardToken: Int,
    val estimatedDurationSec: Int,
    val sortOrder: Int,
    val moduleId: String? = null,
    val audioUrl: String? = null
)
