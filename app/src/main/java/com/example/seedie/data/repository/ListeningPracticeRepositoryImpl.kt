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
        sessionId: String,
        itemRefs: List<String>?
    ): ListeningPracticeSession {
        val allMaterials = remoteDataSource.fetchAllMaterials()
        val questions = remoteDataSource.fetchAllQuestions()
        val options = remoteDataSource.fetchAllOptions()
        val materials = if (itemRefs == null) {
            allMaterials
        } else {
            val order = itemRefs.withIndex().associate { it.value to it.index }
            allMaterials
                .filter { it.material_id in order }
                .sortedBy { order[it.material_id] ?: Int.MAX_VALUE }
        }
        return ListeningSessionAssembler.assemble(
            sessionId = sessionId,
            materials = materials,
            questions = questions,
            options = options
        )
    }
}
