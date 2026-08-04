package com.example.seedie.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WordSenseFormatTest {
    @Test
    fun displayLabel_joinsPosAndTranslationOnOneLine() {
        val label = WordSenseFormat.displayLabel(
            listOf(
                WordSense("v.", "改变"),
                WordSense("n.", "变化")
            )
        )
        assertEquals("v. 改变；n. 变化", label)
    }

    @Test
    fun resolveSenses_fallsBackToLegacyFields() {
        val senses = WordSenseFormat.resolveSenses(
            sensesJson = "[]",
            partOfSpeech = "adj.",
            translation = "快乐的"
        )
        assertEquals(listOf(WordSense("adj.", "快乐的")), senses)
    }

    @Test
    fun parseAndEncode_roundTrip() {
        val original = listOf(WordSense("n.", "书"), WordSense("v.", "预订"))
        val encoded = WordSenseFormat.encodeSensesJson(original)
        assertEquals(original, WordSenseFormat.parseSensesJson(encoded))
    }
}
