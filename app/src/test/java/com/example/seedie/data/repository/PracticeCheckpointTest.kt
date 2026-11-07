package com.example.seedie.data.repository

import com.example.seedie.domain.model.ListeningPlaybackSnapshot
import com.example.seedie.ui.screens.learning.listening.*
import com.example.seedie.ui.screens.learning.practice.*
import com.example.seedie.ui.screens.learning.reading.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class PracticeCheckpointTest {
    @Test fun readingDraftRetainsConfirmedAndUnconfirmedAnswersAndPendingSubmit() {
        val question = ReadingQuestionItem("q1", null, "Question", listOf(
            VocabularyPracticeOption("a", "Answer", true)
        ), "a", "Evidence", null, 1)
        val set = ReadingSetItem("set1", "Title", null, "Passage", listOf(question))
        val checkpoint = ReadingCheckpoint(
            ReadingPracticeSession("stable-attempt", listOf(set)),
            ReadingPracticeUiState(stage = ReadingPracticeStage.Reviewing, currentSet = set,
                answers = mapOf("q1" to "a"), correctCount = 1, earnedDews = 2),
            mapOf("q1" to "a"), "oak", true, false, 0
        )
        val restored = Json.decodeFromString<ReadingCheckpoint>(Json.encodeToString(checkpoint))
        assertEquals(checkpoint, restored)
        assertTrue(restored.submissionPending)
        assertEquals("stable-attempt", restored.session.sessionId)
        assertEquals("Evidence", restored.session.sets.single().questions.single().explanation)
    }

    @Test fun listeningDraftKeepsEvaluatedStateAndPausedAudioPosition() {
        val checkpoint = ListeningCheckpoint(
            ListeningPracticeSession("attempt", emptyList()),
            ListeningPracticeUiState(stage = ListeningPracticeStage.AnswerEvaluated,
                answerStatus = AnswerStatus.Wrong, evaluatedQuestionIds = setOf("q1")),
            mapOf("q1" to "wrong"), mapOf("q2" to "draft"), setOf("q1"),
            "oak", false, false, 0,
            ListeningPlaybackSnapshot("https://example.invalid/audio.mp3", "text", 12345, 0, false)
        )
        val restored = Json.decodeFromString<ListeningCheckpoint>(Json.encodeToString(checkpoint))
        assertEquals(checkpoint, restored)
        assertEquals(12345L, restored.playback?.positionMs)
        assertEquals(AnswerStatus.Wrong, restored.state.answerStatus)
    }
}
