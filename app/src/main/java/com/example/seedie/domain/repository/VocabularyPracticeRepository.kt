package com.example.seedie.domain.repository

import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeSession
import com.example.seedie.ui.screens.learning.practice.VocabularyQuestionRecord

interface VocabularyPracticeRepository {
    suspend fun getPracticeSession(args: VocabularyPracticeArgs): VocabularyPracticeSession

    suspend fun submitQuestionRecord(record: VocabularyQuestionRecord)

    suspend fun finishPracticeSession(result: StudyResult)

    suspend fun getWrongWords(sessionId: String): List<String>
}
