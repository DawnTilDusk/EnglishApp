package com.example.seedie.domain.model

import java.time.Instant
import java.util.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class StudyResultRulesTest {
    private fun result(module: String = "reading") = StudyResult(
        sessionId = "submission-1", moduleId = module, isCompleted = true,
        completedQuestionCount = 3, correctCount = 2, wrongCount = 1, skippedCount = 0,
        accuracy = 2f / 3, earnedTokens = 0, earnedDews = 2, studyDurationSec = 20,
        vocabularyDelta = 0, wrongWordIds = emptyList(), completedAtMillis = 1000
    )

    @Test fun reviewAndIncompleteOrEmptySessionsDoNotCompleteTasks() {
        assertEquals("daily_reading", result().completedDailyTaskKey())
        assertNull(result().copy(countsTowardTasks = false).completedDailyTaskKey())
        assertNull(result().copy(isCompleted = false).completedDailyTaskKey())
        assertNull(result().copy(completedQuestionCount = 0).completedDailyTaskKey())
        assertNull(result("quiz").completedDailyTaskKey())
        assertNull(result("vocabulary").completedDailyTaskKey())
        assertNull(result("vocabulary_review").completedDailyTaskKey())
    }

    @Test fun retriesUseTheSameLedgerReferences() {
        val first = result()
        val retry = first.copy(completedAtMillis = 5000)
        assertEquals(first.tokenRewardRef(), retry.tokenRewardRef())
        assertEquals(first.dewRewardRef(), retry.dewRewardRef())
        assertEquals("assignment:submission-1", first.tokenRewardRef())
        assertEquals("free_reading:item:attempt", first.copy(sessionId = "free_reading:item:attempt").tokenRewardRef())
        assertNotEquals(first.dewRewardRef(), first.copy(sessionId = "other-attempt").dewRewardRef())
    }

    @Test fun persistedResultKeepsCompletionDateAndReviewEligibility() {
        val original = result().copy(countsTowardTasks = false)
        val restored = Json.decodeFromString<StudyResult>(Json.encodeToString(original))
        assertEquals(original, restored)
        assertNull(restored.completedDailyTaskKey())
    }

    @Test fun productDayChangesAtShanghaiMidnightRegardlessOfDeviceTimezone() {
        val prior = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals("2026-11-06", StudyDay.date(Instant.parse("2026-11-06T15:59:59Z").toEpochMilli()))
            assertEquals("2026-11-07", StudyDay.date(Instant.parse("2026-11-06T16:00:00Z").toEpochMilli()))
        } finally { TimeZone.setDefault(prior) }
    }
}
