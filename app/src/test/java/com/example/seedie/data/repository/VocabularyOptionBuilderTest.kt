package com.example.seedie.data.repository

import com.example.seedie.data.local.entity.VocabularyWordEntity
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyOptionBuilderTest {

    private val builder = VocabularyOptionBuilder()

    private val wordBank = listOf(
        word("w1", "apple", "苹果"),
        word("w2", "bridge", "桥"),
        word("w3", "garden", "花园"),
        word("w4", "smile", "微笑")
    )

    @Test
    fun buildEnglishOptions_returnsFourOptionsWithOneCorrect() {
        val options = builder.buildEnglishOptions(
            entity = wordBank.first(),
            allEntries = wordBank,
            random = Random(42)
        )

        assertEquals(4, options.size)
        assertEquals(1, options.count { it.isCorrect })
        assertTrue(options.any { it.label == "apple" && it.isCorrect })
    }

    @Test
    fun buildEnglishOptions_isDeterministicForSameRandomSeed() {
        val first = builder.buildEnglishOptions(wordBank[0], wordBank, Random(7))
        val second = builder.buildEnglishOptions(wordBank[0], wordBank, Random(7))

        assertEquals(first.map { it.optionId }, second.map { it.optionId })
    }

    @Test
    fun buildTranslationOptions_returnsFourOptionsWithOneCorrect() {
        val options = builder.buildTranslationOptions(
            entity = wordBank.first(),
            allEntries = wordBank,
            random = Random(42)
        )

        assertEquals(4, options.size)
        assertEquals(1, options.count { it.isCorrect })
        assertTrue(options.any { it.label == "苹果" && it.isCorrect })
    }

    @Test
    fun buildTranslationOptions_isDeterministicForSameRandomSeed() {
        val first = builder.buildTranslationOptions(wordBank[0], wordBank, Random(7))
        val second = builder.buildTranslationOptions(wordBank[0], wordBank, Random(7))

        assertEquals(first.map { it.optionId }, second.map { it.optionId })
    }

    private fun word(
        wordId: String,
        english: String,
        translation: String
    ): VocabularyWordEntity {
        return VocabularyWordEntity(
            wordId = wordId,
            bookId = "seedie-default-book",
            english = english,
            phonetic = "/test/",
            partOfSpeech = "n.",
            translation = translation,
            exampleSentence = "Example $english",
            difficultyLevel = "easy",
            rewardToken = 3,
            estimatedDurationSec = 8,
            sortOrder = wordId.removePrefix("w").toInt()
        )
    }
}
