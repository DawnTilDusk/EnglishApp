package com.example.seedie.domain.model

data class StudyResult(
    val moduleId: String,
    val isCompleted: Boolean,
    val studyTimeMinutes: Int,
    val newVocabularyCount: Int
)