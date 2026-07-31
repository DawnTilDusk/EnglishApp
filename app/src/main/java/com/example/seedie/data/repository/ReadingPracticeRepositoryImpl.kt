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

    override suspend fun createSession(
        sessionId: String,
        itemRefs: List<String>?
    ): ReadingPracticeSession {
        val allSets = remoteDataSource.fetchAllSets()
        val questions = remoteDataSource.fetchAllQuestions()
        val options = remoteDataSource.fetchAllOptions()
        val sets = if (itemRefs == null) {
            allSets
        } else {
            val order = itemRefs.withIndex().associate { it.value to it.index }
            allSets
                .filter { it.set_id in order }
                .sortedBy { order[it.set_id] ?: Int.MAX_VALUE }
        }
        return ReadingSessionAssembler.assemble(
            sessionId = sessionId,
            sets = sets,
            questions = questions,
            options = options
        )
    }
}
