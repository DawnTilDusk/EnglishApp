package com.example.seedie.data.repository

import com.example.seedie.domain.repository.VocabularyQuizRepository
import com.example.seedie.ui.screens.learning.quiz.VocabularyQuizSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyQuizRepositoryImpl @Inject constructor(
    private val wordBookSeeder: WordBookSeeder,
    private val vocabularyOptionBuilder: VocabularyOptionBuilder
) : VocabularyQuizRepository {

    override suspend fun createSession(
        sessionId: String,
        questionCount: Int,
        difficulty: String
    ): VocabularyQuizSession {
        val book = wordBookSeeder.loadActiveBook()
        val wordBank = wordBookSeeder.loadWordsForBook(book.bookId)
        val filtered = wordBookSeeder.filterByDifficulty(
            wordBank = wordBank,
            requestedDifficulty = difficulty
        )
        return VocabularyQuizSessionFactory.create(
            sessionId = sessionId,
            wordBank = filtered,
            optionBuilder = vocabularyOptionBuilder,
            questionCount = questionCount,
            difficulty = difficulty
        )
    }
}
