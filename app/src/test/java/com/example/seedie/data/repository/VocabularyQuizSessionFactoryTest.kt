package com.example.seedie.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyQuizSessionFactoryTest {

    private val wordBank = VocabularyQuizTestFixtures.wordBank24()
    private val optionBuilder = VocabularyOptionBuilder()

    @Test
    fun createBandQuestions_returnsTwelveQuestions() {
        val questions = VocabularyQuizSessionFactory.createBandQuestions(
            sessionId = "session-test-1",
            bandIndex = 0,
            bookWords = wordBank,
            distractorPool = wordBank,
            optionBuilder = optionBuilder
        )

        assertEquals(12, questions.size)
    }

    @Test
    fun createBandQuestions_eachQuestionHasFourChineseOptionsWithOneCorrect() {
        val questions = VocabularyQuizSessionFactory.createBandQuestions(
            sessionId = "session-test-2",
            bandIndex = 1,
            bookWords = wordBank,
            distractorPool = wordBank,
            optionBuilder = optionBuilder
        )

        questions.forEach { question ->
            assertEquals(4, question.options.size)
            assertEquals(1, question.options.count { it.isCorrect })
            assertTrue(question.options.all { option -> option.label.isNotBlank() })
            val correct = question.options.single { it.isCorrect }
            if (correct.label == com.example.seedie.domain.quiz.VocabularyQuizConstants.NONE_OF_ABOVE_LABEL) {
                assertEquals(question.options.last().optionId, correct.optionId)
                assertTrue(question.options.none { it.label == question.translation })
            } else assertEquals(question.translation, correct.label)
        }
    }

    @Test
    fun createBandQuestions_isDeterministicForSameSessionAndBand() {
        val first = VocabularyQuizSessionFactory.createBandQuestions(
            sessionId = "session-deterministic",
            bandIndex = 2,
            bookWords = wordBank,
            distractorPool = wordBank,
            optionBuilder = optionBuilder
        )
        val second = VocabularyQuizSessionFactory.createBandQuestions(
            sessionId = "session-deterministic",
            bandIndex = 2,
            bookWords = wordBank,
            distractorPool = wordBank,
            optionBuilder = optionBuilder
        )

        assertEquals(first.map { it.wordId }, second.map { it.wordId })
    }
}
