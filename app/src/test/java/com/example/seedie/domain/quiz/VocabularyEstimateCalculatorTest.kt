package com.example.seedie.domain.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyEstimateCalculatorTest {

    @Test
    fun estimate_returns600WhenTotalCountIsZero() {
        val result = VocabularyEstimateCalculator.estimate(
            VocabularyEstimateCalculator.Input(
                correctCount = 0,
                totalCount = 0,
                correctDifficulties = emptyList()
            )
        )
        assertEquals(600, result)
    }

    @Test
    fun estimate_allWrong_returnsBetween600And800() {
        val result = VocabularyEstimateCalculator.estimate(
            VocabularyEstimateCalculator.Input(
                correctCount = 0,
                totalCount = 12,
                correctDifficulties = emptyList()
            )
        )
        assertTrue(result in 600..800)
        assertEquals(0, result % 50)
    }

    @Test
    fun estimate_allCorrect_returnsAtLeast2000() {
        val difficulties = buildList {
            repeat(4) { add("easy") }
            repeat(4) { add("medium") }
            repeat(4) { add("hard") }
        }
        val result = VocabularyEstimateCalculator.estimate(
            VocabularyEstimateCalculator.Input(
                correctCount = 12,
                totalCount = 12,
                correctDifficulties = difficulties
            )
        )
        assertTrue(result >= 2000)
        assertEquals(0, result % 50)
    }

    @Test
    fun estimate_isDeterministicForSameInput() {
        val input = VocabularyEstimateCalculator.Input(
            correctCount = 8,
            totalCount = 12,
            correctDifficulties = listOf("easy", "easy", "medium", "medium", "hard", "hard", "hard", "hard")
        )
        assertEquals(
            VocabularyEstimateCalculator.estimate(input),
            VocabularyEstimateCalculator.estimate(input)
        )
    }

    @Test
    fun estimate_higherCorrectCountIsMonotonic() {
        val difficulties = listOf("easy", "medium", "hard", "hard")
        val lower = VocabularyEstimateCalculator.estimate(
            VocabularyEstimateCalculator.Input(
                correctCount = 2,
                totalCount = 12,
                correctDifficulties = difficulties.take(2)
            )
        )
        val higher = VocabularyEstimateCalculator.estimate(
            VocabularyEstimateCalculator.Input(
                correctCount = 4,
                totalCount = 12,
                correctDifficulties = difficulties
            )
        )
        assertTrue(higher >= lower)
    }
}
