package com.example.seedie.ui.screens.learning.practice

import org.junit.Assert.assertEquals
import org.junit.Test

class VocabularyPracticeProgressTest {
    @Test
    fun partialStudyUsesWholeRoundTarget() {
        val state = VocabularyPracticeUiState(studyTargetCount = 8, masteredStudyCount = 2)
        assertEquals(6, state.remainingWordCount)
    }

    @Test
    fun resumedStudyDoesNotUseActiveQueueAsTarget() {
        val state = VocabularyPracticeUiState(
            studyTargetCount = 10, masteredStudyCount = 2, studyQueueSize = 4
        )
        assertEquals(8, state.remainingWordCount)
    }

    @Test
    fun reviewCountsOnlyWordsStillInQueue() {
        val state = VocabularyPracticeUiState(
            currentSection = VocabularyPracticeMode.Review,
            reviewQueueSize = 3,
            completedReviewCount = 2,
            sentBackToStudyCount = 1
        )
        assertEquals(3, state.remainingWordCount)
    }

    @Test
    fun completedStudyHasNoRemainingWords() {
        assertEquals(
            0,
            VocabularyPracticeUiState(studyTargetCount = 8, masteredStudyCount = 8).remainingWordCount
        )
    }
}
