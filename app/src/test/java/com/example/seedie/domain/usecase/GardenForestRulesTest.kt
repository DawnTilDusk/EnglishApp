package com.example.seedie.domain.usecase

import com.example.seedie.data.local.entity.GardenPlantEntity
import com.example.seedie.domain.model.GardenSpeciesCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GardenForestRulesTest {

    @Test
    fun recordDecision_skipsCompletedWithZeroQuestions() {
        assertNull(statusFor(completed = true, questions = 0))
    }

    @Test
    fun recordDecision_witheredOnAbandonEvenWithZeroQuestions() {
        assertEquals(GardenPlantEntity.STATUS_WITHERED, statusFor(completed = false, questions = 0))
    }

    @Test
    fun recordDecision_aliveWhenCompletedWithQuestions() {
        assertEquals(GardenPlantEntity.STATUS_ALIVE, statusFor(completed = true, questions = 3))
    }

    @Test
    fun recordDecision_witheredWhenAbandonedWithQuestions() {
        assertEquals(GardenPlantEntity.STATUS_WITHERED, statusFor(completed = false, questions = 2))
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

    private fun statusFor(completed: Boolean, questions: Int): String? {
        return when {
            !completed -> GardenPlantEntity.STATUS_WITHERED
            questions > 0 -> GardenPlantEntity.STATUS_ALIVE
            else -> null
        }
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
