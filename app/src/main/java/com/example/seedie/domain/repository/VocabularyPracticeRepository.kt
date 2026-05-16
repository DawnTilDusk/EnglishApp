package com.example.seedie.domain.repository

import com.example.seedie.domain.model.StudyResult
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.PendingReviewEntry
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeResumeSnapshot
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeSession
import com.example.seedie.ui.screens.learning.practice.VocabularyQuestionRecord

interface VocabularyPracticeRepository {
    suspend fun getPracticeSession(args: VocabularyPracticeArgs): VocabularyPracticeSession

    suspend fun saveStudyRoundSnapshot(snapshot: VocabularyPracticeResumeSnapshot)

    suspend fun markStudyRoundReviewPending(roundId: String)

    suspend fun markReviewCompleted(roundId: String)

    suspend fun getPendingReviewEntry(): PendingReviewEntry?

    suspend fun submitQuestionRecord(record: VocabularyQuestionRecord)

    suspend fun finishPracticeSession(result: StudyResult)

    suspend fun getWrongWords(sessionId: String): List<String>
}
