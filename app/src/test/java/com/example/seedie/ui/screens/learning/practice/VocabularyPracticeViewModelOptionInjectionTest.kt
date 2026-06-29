package com.example.seedie.ui.screens.learning.practice

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyPracticeViewModelOptionInjectionTest {

    @Test
    fun maybeInjectNoneOfAboveOption_keepsStageOneUnchanged() {
        val baseOptions = baseOptions()

        val result = maybeInjectNoneOfAboveOption(
            questionType = VocabularyQuestionType.StudyEnglishToChinese,
            baseOptions = baseOptions,
            random = Random(0)
        )

        assertEquals(baseOptions, result)
    }

    @Test
    fun maybeInjectNoneOfAboveOption_replacesOneStageTwoOptionButKeepsIdentity() {
        val baseOptions = baseOptions()
        val injectedResult = findInjectedResult(
            questionType = VocabularyQuestionType.StudyChineseToEnglish,
            baseOptions = baseOptions
        )

        assertNotNull(injectedResult)
        val result = injectedResult!!.result
        val replacedIndex = result.indexOfFirst { it.label == "全都不对" }

        assertEquals(4, result.size)
        assertEquals(1, result.count { it.label == "全都不对" })
        assertTrue(replacedIndex in result.indices)
        assertEquals(baseOptions[replacedIndex].optionId, result[replacedIndex].optionId)
        assertEquals(baseOptions[replacedIndex].isCorrect, result[replacedIndex].isCorrect)
        assertFalse(result[replacedIndex].showFeedbackHint)
    }

    @Test
    fun maybeInjectNoneOfAboveOption_replacesOneStageThreeOptionButKeepsIdentity() {
        val baseOptions = baseOptions()
        val injectedResult = findInjectedResult(
            questionType = VocabularyQuestionType.StudyContextChoice,
            baseOptions = baseOptions
        )

        assertNotNull(injectedResult)
        val result = injectedResult!!.result
        val replacedIndex = result.indexOfFirst { it.label == "全都不对" }

        assertEquals(4, result.size)
        assertEquals(1, result.count { it.label == "全都不对" })
        assertTrue(replacedIndex in result.indices)
        assertEquals(baseOptions[replacedIndex].optionId, result[replacedIndex].optionId)
        assertEquals(baseOptions[replacedIndex].isCorrect, result[replacedIndex].isCorrect)
        assertFalse(result[replacedIndex].showFeedbackHint)
    }

    private fun findInjectedResult(
        questionType: VocabularyQuestionType,
        baseOptions: List<VocabularyPracticeOption>
    ): InjectedResult? {
        return (0..200)
            .asSequence()
            .map { seed ->
                val result = maybeInjectNoneOfAboveOption(
                    questionType = questionType,
                    baseOptions = baseOptions,
                    random = Random(seed)
                )
                InjectedResult(seed = seed, result = result)
            }
            .firstOrNull { candidate -> candidate.result.any { it.label == "全都不对" } }
    }

    private fun baseOptions(): List<VocabularyPracticeOption> {
        return listOf(
            VocabularyPracticeOption(
                optionId = "opt_a",
                label = "A",
                isCorrect = false,
                englishHint = "hint-a"
            ),
            VocabularyPracticeOption(
                optionId = "opt_b",
                label = "B",
                isCorrect = true,
                englishHint = "hint-b"
            ),
            VocabularyPracticeOption(
                optionId = "opt_c",
                label = "C",
                isCorrect = false,
                englishHint = "hint-c"
            ),
            VocabularyPracticeOption(
                optionId = "opt_d",
                label = "D",
                isCorrect = false,
                englishHint = "hint-d"
            )
        )
    }

    private data class InjectedResult(
        val seed: Int,
        val result: List<VocabularyPracticeOption>
    )
}
