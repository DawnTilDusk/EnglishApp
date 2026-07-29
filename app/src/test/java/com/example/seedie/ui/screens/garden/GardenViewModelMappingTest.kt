package com.example.seedie.ui.screens.garden

import com.example.seedie.domain.model.ActivityModule
import com.example.seedie.domain.model.ActivityModuleSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenViewModelMappingTest {

    @Test
    fun toGardenStatsUiState_filtersExcludedModulesAndOrdersByDuration() {
        val stats = toGardenStatsUiState(
            listOf(
                ActivityModuleSummary(moduleId = ActivityModule.VocabularyQuiz.id, durationSec = 900),
                ActivityModuleSummary(moduleId = ActivityModule.Dashboard.id, durationSec = 300),
                ActivityModuleSummary(moduleId = ActivityModule.ReadingPractice.id, durationSec = 600),
                ActivityModuleSummary(moduleId = ActivityModule.ListeningPractice.id, durationSec = 600)
            )
        )

        assertTrue(stats.learningDistribution.hasData)
        assertEquals(2_100, stats.learningDistribution.totalDurationSec)
        assertEquals(
            listOf("词汇测验", "听力训练", "阅读训练"),
            stats.learningDistribution.items.map { it.label }
        )
    }

    @Test
    fun toGardenStatsUiState_returnsEmptyWhenNoLearningData() {
        val stats = toGardenStatsUiState(
            listOf(
                ActivityModuleSummary(moduleId = ActivityModule.Dashboard.id, durationSec = 120),
                ActivityModuleSummary(moduleId = ActivityModule.Shop.id, durationSec = 60)
            )
        )

        assertFalse(stats.learningDistribution.hasData)
        assertEquals(0, stats.learningDistribution.totalDurationSec)
        assertTrue(stats.learningDistribution.items.isEmpty())
    }
}
