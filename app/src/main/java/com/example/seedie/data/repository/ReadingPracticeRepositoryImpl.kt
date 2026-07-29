package com.example.seedie.data.repository

import com.example.seedie.data.remote.ReadingRemoteDataSource
import com.example.seedie.domain.repository.ReadingPracticeRepository
import com.example.seedie.ui.screens.learning.reading.ReadingPracticeSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingPracticeRepositoryImpl @Inject constructor(
    private val remoteDataSource: ReadingRemoteDataSource
) : ReadingPracticeRepository {

    override suspend fun createSession(sessionId: String): ReadingPracticeSession {
        val sets = remoteDataSource.fetchAllSets()
        val questions = remoteDataSource.fetchAllQuestions()
        val options = remoteDataSource.fetchAllOptions()
        return ReadingSessionAssembler.assemble(
            sessionId = sessionId,
            sets = sets,
            questions = questions,
            options = options
        )
    }
}
