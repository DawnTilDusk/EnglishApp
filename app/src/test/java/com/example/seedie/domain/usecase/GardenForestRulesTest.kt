package com.example.seedie.domain.usecase

import com.example.seedie.data.local.entity.GardenPlantEntity
import com.example.seedie.domain.model.GardenSpeciesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenForestRulesTest {

    @Test
    fun statusFor_skipsCompletedWithZeroQuestions() {
        assertNull(
            GardenForestRules.statusFor(
                isCompleted = true,
                questionCount = 0,
                abandonElapsedMs = GardenForestRules.ABANDON_GRACE_MS
            )
        )
    }

    @Test
    fun statusFor_skipsAbandonWithinGraceEvenWithAnswers() {
        assertNull(
            GardenForestRules.statusFor(
                isCompleted = false,
                questionCount = 3,
                abandonElapsedMs = GardenForestRules.ABANDON_GRACE_MS - 1
            )
        )
    }

    @Test
    fun statusFor_skipsAbandonWithinGraceWithZeroQuestions() {
        assertNull(
            GardenForestRules.statusFor(
                isCompleted = false,
                questionCount = 0,
                abandonElapsedMs = 0L
            )
        )
    }

    @Test
    fun statusFor_witheredWhenAbandonPastGraceWithZeroQuestions() {
        assertEquals(
            GardenPlantEntity.STATUS_WITHERED,
            GardenForestRules.statusFor(
                isCompleted = false,
                questionCount = 0,
                abandonElapsedMs = GardenForestRules.ABANDON_GRACE_MS
            )
        )
    }

    @Test
    fun statusFor_witheredWhenAbandonPastGraceWithAnswers() {
        assertEquals(
            GardenPlantEntity.STATUS_WITHERED,
            GardenForestRules.statusFor(
                isCompleted = false,
                questionCount = 2,
                abandonElapsedMs = GardenForestRules.ABANDON_GRACE_MS
            )
        )
    }

    @Test
    fun statusFor_aliveWhenCompletedWithQuestionsRegardlessOfElapsed() {
        assertEquals(
            GardenPlantEntity.STATUS_ALIVE,
            GardenForestRules.statusFor(
                isCompleted = true,
                questionCount = 3,
                abandonElapsedMs = 0L
            )
        )
        assertEquals(
            GardenPlantEntity.STATUS_ALIVE,
            GardenForestRules.statusFor(
                isCompleted = true,
                questionCount = 3,
                abandonElapsedMs = GardenForestRules.ABANDON_GRACE_MS * 2
            )
        )
    }

    @Test
    fun isWithinAbandonGrace_falseWhenOpenedAtZero() {
        assertFalse(GardenForestRules.isWithinAbandonGrace(0L, nowMillis = 1_000L))
    }

    @Test
    fun isWithinAbandonGrace_trueInsideWindow() {
        val opened = 1_000L
        assertTrue(
            GardenForestRules.isWithinAbandonGrace(
                sessionOpenedAtMillis = opened,
                nowMillis = opened + GardenForestRules.ABANDON_GRACE_MS - 1
            )
        )
    }

    @Test
    fun abandonElapsedMs_treatsZeroAsPastGrace() {
        assertEquals(
            GardenForestRules.ABANDON_GRACE_MS,
            GardenForestRules.abandonElapsedMs(0L, nowMillis = 5_000L)
        )
    }

    @Test
    fun forestLayout_isUniformSixBySix() {
        val scene = ForestLayout.build(emptyList())
        assertEquals(36, scene.cells.size)
        assertEquals(ForestLayout.GRID_SIZE, 6)
        assertEquals(36, scene.cells.map { it.row to it.col }.toSet().size)
        assertTrue(scene.cells.all { it.row in 0 until 6 && it.col in 0 until 6 })
    }

    @Test
    fun forestLayout_placementIsStableAcrossRebuild() {
        val plants = listOf(
            plant("a", "session-aaa", createdAt = 1L),
            plant("b", "session-bbb", createdAt = 2L),
            plant("c", "session-ccc", createdAt = 3L)
        )
        val first = ForestLayout.build(plants)
        val second = ForestLayout.build(plants)
        assertEquals(3, first.trees.size)
        assertEquals(
            first.trees.map { it.plantId to (it.row to it.col) }.toSet(),
            second.trees.map { it.plantId to (it.row to it.col) }.toSet()
        )
    }

    @Test
    fun forestLayout_addingPlantDoesNotMoveExisting() {
        val firstThree = listOf(
            plant("a", "session-aaa", createdAt = 1L),
            plant("b", "session-bbb", createdAt = 2L),
            plant("c", "session-ccc", createdAt = 3L)
        )
        val before = ForestLayout.build(firstThree)
        val positionsBefore = before.trees.associate { it.plantId to (it.row to it.col) }

        val after = ForestLayout.build(
            firstThree + plant("d", "session-ddd", createdAt = 4L)
        )
        val positionsAfter = after.trees.associate { it.plantId to (it.row to it.col) }

        assertEquals(4, after.trees.size)
        assertEquals(positionsBefore["a"], positionsAfter["a"])
        assertEquals(positionsBefore["b"], positionsAfter["b"])
        assertEquals(positionsBefore["c"], positionsAfter["c"])
        assertTrue(positionsAfter.containsKey("d"))
    }

    @Test
    fun preferredSlot_isDeterministic() {
        assertEquals(ForestLayout.preferredSlot("plant-xyz"), ForestLayout.preferredSlot("plant-xyz"))
        assertTrue(ForestLayout.preferredSlot("plant-xyz") in 0 until ForestLayout.totalCells)
    }

    @Test
    fun defaultSpeciesExists() {
        assertEquals("嫩芽", GardenSpeciesCatalog.requireById(GardenSpeciesCatalog.DEFAULT_SPECIES_ID).displayName)
    }

    private fun plant(id: String, sessionId: String, createdAt: Long = 1L) = GardenPlantEntity(
        id = id,
        userId = "u1",
        sessionId = sessionId,
        moduleId = "vocabulary",
        speciesId = GardenSpeciesCatalog.DEFAULT_SPECIES_ID,
        status = GardenPlantEntity.STATUS_ALIVE,
        completedQuestionCount = 5,
        correctCount = 4,
        studyDurationSec = 60,
        createdAt = createdAt,
        localDate = "2026-08-06"
    )
}
