package com.example.seedie.domain.quiz

import com.example.seedie.data.repository.VocabularyQuizTestFixtures
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyQuizWordSelectorTest {

    private val wordBank = VocabularyQuizTestFixtures.wordBank24()

    @Test
    fun selectFromBook_returnsRequestedCount() {
        val selected = VocabularyQuizWordSelector.selectFromBook(
            wordBank = wordBank,
            random = Random(3),
            count = 12
        )
        assertEquals(12, selected.size)
        assertEquals(selected.size, selected.distinctBy { it.wordId }.size)
    }

    @Test
    fun selectBalanced_returnsFourWordsPerDifficulty() {
        val selected = VocabularyQuizWordSelector.selectBalanced(
            wordBank = wordBank,
            random = Random(42)
        )

        assertEquals(12, selected.size)
        assertEquals(4, selected.count { it.difficultyLevel == "easy" })
        assertEquals(4, selected.count { it.difficultyLevel == "medium" })
        assertEquals(4, selected.count { it.difficultyLevel == "hard" })
        assertEquals(selected.size, selected.distinctBy { it.wordId }.size)
    }

    @Test
    fun selectBalanced_isDeterministicForSameSeed() {
        val first = VocabularyQuizWordSelector.selectBalanced(wordBank, Random(7))
        val second = VocabularyQuizWordSelector.selectBalanced(wordBank, Random(7))

        assertEquals(first.map { it.wordId }, second.map { it.wordId })
    }

    @Test
    fun selectRandom_respectsQuestionCount() {
        val selected = VocabularyQuizWordSelector.selectRandom(
            wordBank = wordBank,
            random = Random(11),
            questionCount = 6
        )

        assertEquals(6, selected.size)
    }
}
