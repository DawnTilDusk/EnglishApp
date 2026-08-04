package com.example.seedie.domain.repository

import com.example.seedie.data.local.entity.VocabularyWordEntity

data class VocabularyQuizWordPool(
    val sessionId: String,
    val wordsByBookId: Map<String, List<VocabularyWordEntity>>
)

interface VocabularyQuizRepository {
    /**
     * Loads all FLTRP junior books for grade-band quiz (Room first, remote fill).
     * Throws if any required book cannot provide enough words.
     */
    suspend fun loadWordPool(sessionId: String): VocabularyQuizWordPool
}
