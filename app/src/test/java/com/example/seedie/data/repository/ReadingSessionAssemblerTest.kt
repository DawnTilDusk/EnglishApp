package com.example.seedie.data.repository

import com.example.seedie.data.remote.SupabaseReadingOption
import com.example.seedie.data.remote.SupabaseReadingQuestion
import com.example.seedie.data.remote.SupabaseReadingSet
import com.example.seedie.domain.reading.ReadingPracticeConstants
import com.example.seedie.domain.reading.ReadingPracticeScorer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingSessionAssemblerTest {

    @Test
    fun assemble_ordersSetsQuestionsAndOptions() {
        val session = ReadingSessionAssembler.assemble(
            sessionId = "s1",
            sets = listOf(
                set(id = "r08-02", sort = 1, title = "Second"),
                set(id = "r08-01", sort = 0, title = "First")
            ),
            questions = listOf(
                question(id = "q2", setId = "r08-01", sort = 1, correct = "B"),
                question(id = "q1", setId = "r08-01", sort = 0, correct = "A"),
                question(id = "q3", setId = "r08-02", sort = 0, correct = "C")
            ),
            options = listOf(
                option("q1", "B", "Wrong", 1),
                option("q1", "A", "Right", 0),
                option("q2", "B", "Right", 0),
                option("q2", "A", "Wrong", 1),
                option("q3", "C", "Right", 0),
                option("q3", "A", "Wrong", 1)
            )
        )

        assertEquals("s1", session.sessionId)
        assertEquals(listOf("r08-01", "r08-02"), session.sets.map { it.setId })
        assertEquals(listOf("q1", "q2"), session.sets[0].questions.map { it.questionId })
        assertEquals("A", session.sets[0].questions[0].options.first().optionId)
        assertTrue(session.sets[0].questions[0].options.first().isCorrect)
        assertEquals(1, session.sets[0].questions[0].options.count { it.isCorrect })
    }

    @Test
    fun scoreSet_sumsRewardTokensForCorrectAnswers() {
        val session = ReadingSessionAssembler.assemble(
            sessionId = "s2",
            sets = listOf(set(id = "r08-01", sort = 0, title = "First")),
            questions = listOf(
                question(id = "q1", setId = "r08-01", sort = 0, correct = "A", reward = 2),
                question(id = "q2", setId = "r08-01", sort = 1, correct = "B", reward = 2)
            ),
            options = listOf(
                option("q1", "A", "Right", 0),
                option("q1", "B", "Wrong", 1),
                option("q2", "A", "Wrong", 0),
                option("q2", "B", "Right", 1)
            )
        )
        val score = ReadingPracticeScorer.scoreSet(
            set = session.sets.first(),
            answers = mapOf("q1" to "A", "q2" to "A")
        )
        assertEquals(1, score.correctCount)
        assertEquals(1, score.wrongCount)
        assertEquals(2, score.earnedTokens)
        assertEquals(5, ReadingPracticeConstants.COMPLETION_BONUS)
    }

    private fun set(id: String, sort: Int, title: String) = SupabaseReadingSet(
        set_id = id,
        title = title,
        title_zh = null,
        passage = "Hello <<world>>.",
        sort_order = sort
    )

    private fun question(
        id: String,
        setId: String,
        sort: Int,
        correct: String,
        reward: Int = 2
    ) = SupabaseReadingQuestion(
        question_id = id,
        set_id = setId,
        question_type = "detail",
        stem = "Stem $id",
        sort_order = sort,
        correct_option_id = correct,
        explanation = "Because",
        highlight_word = null,
        reward_token = reward
    )

    private fun option(
        questionId: String,
        optionId: String,
        text: String,
        sort: Int
    ) = SupabaseReadingOption(
        question_id = questionId,
        option_id = optionId,
        option_text = text,
        sort_order = sort
    )
}
