package com.example.seedie.ui.screens.garden

import com.example.seedie.domain.model.ActivityModule
import com.example.seedie.domain.model.ActivityModuleSummary
import com.example.seedie.domain.model.DailyActivityTotal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenViewModelMappingTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2026-07-01T08:00:00Z"),
        ZoneId.of("UTC")
    )

    @Test
    fun buildGardenActivityStats_buildsTodaySummaryAndSevenDayTrend() {
        val stats = buildGardenActivityStats(
            todayModules = listOf(
                ActivityModuleSummary(moduleId = ActivityModule.VocabularyQuiz.id, durationSec = 900),
                ActivityModuleSummary(moduleId = ActivityModule.Dashboard.id, durationSec = 300)
            ),
            recentTotals = listOf(
                DailyActivityTotal(date = "2026-06-25", durationSec = 600),
                DailyActivityTotal(date = "2026-06-27", durationSec = 1_200),
                DailyActivityTotal(date = "2026-07-01", durationSec = 1_800)
            ),
            clock = clock
        )

        assertTrue(stats.hasActivity)
        assertEquals(1_200, stats.totalActiveDurationSec)
        assertEquals(
            listOf("词汇测验", "首页"),
            stats.moduleDurations.map { it.label }
        )
        assertEquals(7, stats.trendPoints.size)
        assertEquals(
            listOf(600, 0, 1_200, 0, 0, 0, 1_800),
            stats.trendPoints.map { it.durationSec }
        )
        assertEquals("今天", stats.trendPoints.last().label)
    }

    @Test
    fun buildGardenActivityStats_returnsZeroStateWhenNoDataExists() {
        val stats = buildGardenActivityStats(
            todayModules = emptyList(),
            recentTotals = emptyList(),
            clock = clock
        )

        assertFalse(stats.hasActivity)
        assertEquals(0, stats.totalActiveDurationSec)
        assertTrue(stats.moduleDurations.isEmpty())
        assertEquals(List(7) { 0 }, stats.trendPoints.map { it.durationSec })
    }
}
