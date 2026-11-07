package com.example.seedie.data.local.entity

import androidx.room.Entity

@Entity(tableName = "daily_word_completions", primaryKeys = ["userId", "date", "bookId", "wordId"])
data class DailyWordCompletionEntity(
    val userId: String,
    val date: String,
    val bookId: String,
    val wordId: String,
    val completedAt: Long
)

@Entity(tableName = "study_settlements", primaryKeys = ["userId", "settlementId"])
data class StudySettlementEntity(
    val userId: String,
    val settlementId: String,
    val payload: String,
    val createdAt: Long,
    val settled: Boolean = false
)

@Entity(tableName = "practice_drafts", primaryKeys = ["userId", "draftKey"])
data class PracticeDraftEntity(
    val userId: String,
    val draftKey: String,
    val payload: String,
    val updatedAt: Long
)
