package com.example.seedie.data.repository

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyQuizSessionFactoryTest {

    private val wordBank = VocabularyQuizTestFixtures.wordBank24()
    private val optionBuilder = VocabularyOptionBuilder()

    @Test
    fun create_returnsTwelveQuestionsWithBalancedDifficulty() {
        val session = VocabularyQuizSessionFactory.create(
            sessionId = "session-test-1",
            wordBank = wordBank,
            optionBuilder = optionBuilder
        )

        assertEquals(12, session.questions.size)
        assertEquals(4, session.questions.count { it.difficultyLevel == "easy" })
        assertEquals(4, session.questions.count { it.difficultyLevel == "medium" })
        assertEquals(4, session.questions.count { it.difficultyLevel == "hard" })
    }

    @Test
    fun create_eachQuestionHasFourChineseOptionsWithOneCorrect() {
        val session = VocabularyQuizSessionFactory.create(
            sessionId = "session-test-2",
            wordBank = wordBank,
            optionBuilder = optionBuilder
        )

        session.questions.forEach { question ->
            assertEquals(4, question.options.size)
            assertEquals(1, question.options.count { it.isCorrect })
            assertTrue(question.options.all { option -> option.label.isNotBlank() })
            assertTrue(question.options.any { it.label == question.translation && it.isCorrect })
        }
    }

    @Test
    fun create_isDeterministicForSameSessionId() {
        val first = VocabularyQuizSessionFactory.create(
            sessionId = "session-deterministic",
            wordBank = wordBank,
            optionBuilder = optionBuilder
        )
        val second = VocabularyQuizSessionFactory.create(
            sessionId = "session-deterministic",
            wordBank = wordBank,
            optionBuilder = optionBuilder
        )

        assertEquals(first.questions.map { it.wordId }, second.questions.map { it.wordId })
    }

    @Test
    fun create_singleDifficultyUsesRandomSelection() {
        val easyWords = wordBank.filter { it.difficultyLevel == "easy" }
        val session = VocabularyQuizSessionFactory.create(
            sessionId = "session-easy",
            wordBank = easyWords,
            optionBuilder = optionBuilder,
            questionCount = 6,
            difficulty = "easy"
        )

        assertEquals(6, session.questions.size)
        assertTrue(session.questions.all { it.difficultyLevel == "easy" })
    }
}
