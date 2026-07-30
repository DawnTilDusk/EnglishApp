package com.example.seedie.data.repository

import com.example.seedie.data.remote.ListeningRemoteDataSource
import com.example.seedie.domain.repository.ListeningPracticeRepository
import com.example.seedie.ui.screens.learning.listening.ListeningPracticeSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListeningPracticeRepositoryImpl @Inject constructor(
    private val remoteDataSource: ListeningRemoteDataSource
) : ListeningPracticeRepository {

    override suspend fun createSession(
        questionCount: Int,
        difficulty: String,
        sessionId: String
    ): ListeningPracticeSession {
        val materials = remoteDataSource.fetchAllMaterials()
        val questions = remoteDataSource.fetchAllQuestions()
        val options = remoteDataSource.fetchAllOptions()
        return ListeningSessionAssembler.assemble(
            sessionId = sessionId,
            materials = materials,
            questions = questions,
            options = options
        )
    }
}
