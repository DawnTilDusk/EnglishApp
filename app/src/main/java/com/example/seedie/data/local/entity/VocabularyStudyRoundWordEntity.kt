package com.example.seedie.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "vocabulary_study_round_words",
    primaryKeys = ["roundId", "wordId"],
    indices = [
        Index(value = ["roundId"]),
        Index(value = ["roundId", "queueOrder"])
    ]
)
data class VocabularyStudyRoundWordEntity(
    val roundId: String,
    val wordId: String,
    val queueOrder: Int,
    val passedStages: String,
    val hasSeenStudyWord: Boolean,
    val totalWrongCount: Int,
    val revealCount: Int,
    val isMasteredInRound: Boolean,
    val updatedAt: Long
)
