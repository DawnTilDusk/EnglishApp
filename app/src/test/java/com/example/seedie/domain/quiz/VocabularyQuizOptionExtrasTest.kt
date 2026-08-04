package com.example.seedie.domain.quiz

import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeOption
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyQuizOptionExtrasTest {

    private fun fourOptions(fourthCorrect: Boolean): List<VocabularyPracticeOption> {
        return listOf(
            VocabularyPracticeOption("o1", "甲", isCorrect = !fourthCorrect),
            VocabularyPracticeOption("o2", "乙", isCorrect = false),
            VocabularyPracticeOption("o3", "丙", isCorrect = false),
            VocabularyPracticeOption("o4", "丁", isCorrect = fourthCorrect)
        )
    }

    @Test
    fun inject_whenRandomBelowProbability_replacesFourthLabel() {
        // nextFloat() from Random(1) is < 0.4 for this seed on Kotlin/JVM
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextFloat(): Float = 0.1f
        }
        val result = maybeReplaceFourthWithNoneOfAbove(
            options = fourOptions(fourthCorrect = false),
            random = random,
            probability = 0.4f
        )
        assertEquals(VocabularyQuizConstants.NONE_OF_ABOVE_LABEL, result[3].label)
        assertFalse(result[3].isCorrect)
        assertEquals("甲", result[0].label)
    }

    @Test
    fun inject_whenFourthWasCorrect_noneOfAboveIsCorrect() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextFloat(): Float = 0.1f
        }
        val result = maybeReplaceFourthWithNoneOfAbove(
            options = fourOptions(fourthCorrect = true),
            random = random
        )
        assertEquals(VocabularyQuizConstants.NONE_OF_ABOVE_LABEL, result[3].label)
        assertTrue(result[3].isCorrect)
        assertEquals(1, result.count { it.isCorrect })
    }

    @Test
    fun skip_whenRandomAboveProbability() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextFloat(): Float = 0.9f
        }
        val base = fourOptions(fourthCorrect = false)
        val result = maybeReplaceFourthWithNoneOfAbove(base, random)
        assertEquals(base.map { it.label }, result.map { it.label })
    }

    @Test
    fun skip_whenFewerThanFourOptions() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextFloat(): Float = 0.0f
        }
        val three = fourOptions(false).take(3)
        assertEquals(three, maybeReplaceFourthWithNoneOfAbove(three, random))
    }
}
