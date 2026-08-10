package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary_words",
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "difficultyLevel"]),
        Index(value = ["moduleId"]),
        Index(value = ["masterId"])
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
    val audioUrl: String? = null,
    /** JSON array of {part_of_speech, translation}; empty → use legacy fields. */
    val sensesJson: String = "[]",
    /** Numeric grade-based difficulty, inherited from owning word_book. Nullable
     *  when server has not populated it yet; readers should fall back to the
     *  legacy `difficultyLevel` enum. */
    val difficultyValue: Int? = null,
    /** Reference into vocabulary_master; nullable for legacy rows. */
    val masterId: String? = null,
    /** 例句对应的中文；空串表示服务端尚未补齐。 */
    val exampleTranslation: String = ""
)
