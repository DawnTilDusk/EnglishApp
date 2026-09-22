package com.example.seedie.ui.screens.learning.listening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningEvidenceTest {
    @Test
    fun sharedEvidencePreservesTranscriptAndLinksEveryRelatedQuestion() {
        val text = "First sentence.\nWe meet at five. Last sentence."
        val segments = listeningTranscriptSegments(text, listOf("q1", "q2"), mapOf(
            "q1" to listOf("We meet at five."), "q2" to listOf("We meet at five.")
        ))
        assertEquals(text, segments.joinToString("") { it.text })
        assertEquals(listOf("q1", "q2"), segments.single { it.text == "We meet at five." }.questionIds)
    }

    @Test
    fun staleEvidenceAndQuestionsFromOtherMaterialsAreNotHighlighted() {
        val segments = listeningTranscriptSegments("Changed text.", listOf("q1"), mapOf(
            "q1" to listOf("Old text."), "q2" to listOf("Changed text.")
        ))
        assertEquals("Changed text.", segments.joinToString("") { it.text })
        assertTrue(segments.all { it.questionIds.isEmpty() })
    }

    @Test
    fun overlappingEvidenceRetainsBothLinksWithoutRepeatingText() {
        val segments = listeningTranscriptSegments("We meet at five.", listOf("q1", "q2"), mapOf(
            "q1" to listOf("We meet at five."), "q2" to listOf("at five")
        ))
        assertEquals("We meet at five.", segments.joinToString("") { it.text })
        assertEquals(listOf("q1", "q2"), segments.single { it.text == "at five" }.questionIds)
    }

    @Test
    fun transcriptPageIsAddedOnlyForReview() {
        assertEquals(3, listeningReviewPageCount(3, false))
        assertEquals(4, listeningReviewPageCount(3, true))
        assertEquals(39, listeningEvidenceQuotes.size)
    }
}
