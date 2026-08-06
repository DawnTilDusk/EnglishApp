package com.example.seedie.domain.usecase

import com.example.seedie.data.local.entity.GardenPlantEntity
import com.example.seedie.domain.model.GardenSpeciesCatalog
import kotlin.math.abs

data class PlacedTree(
    val plantId: String,
    val speciesId: String,
    val status: String,
    val moduleId: String,
    val completedQuestionCount: Int,
    val studyDurationSec: Int,
    val row: Int,
    val col: Int,
    /** Depth 0 (far) → 1 (near). */
    val depth: Float,
    val scale: Float
)

data class ForestGridCell(
    val row: Int,
    val col: Int,
    /** Depth 0 (far) → 1 (near). */
    val depth: Float,
    val scale: Float,
    val tree: PlacedTree?
)

data class ForestSceneModel(
    val cells: List<ForestGridCell>,
    val trees: List<PlacedTree>
)

object ForestLayout {
    const val GRID_SIZE: Int = 6

    val totalCells: Int
        get() = GRID_SIZE * GRID_SIZE

    fun depthOf(col: Int, row: Int): Float {
        val denom = 2f * (GRID_SIZE - 1).coerceAtLeast(1)
        return ((col + row) / denom).coerceIn(0f, 1f)
    }

    /** Depth 0 (far) → 1 (near). Strong near-large / far-small for sprites. */
    fun scaleOf(depth: Float): Float = lerp(0.48f, 1.28f, depth)

    fun build(plants: List<GardenPlantEntity>): ForestSceneModel {
        val capacity = totalCells
        val visible = if (plants.size > capacity) {
            plants.sortedBy { it.createdAt }.takeLast(capacity)
        } else {
            plants.sortedBy { it.createdAt }
        }

        val occupancy = HashMap<Int, GardenPlantEntity>(visible.size)
        for (plant in visible) {
            val slot = allocateSlot(plant.id, occupancy.keys)
            occupancy[slot] = plant
        }

        val cells = ArrayList<ForestGridCell>(capacity)
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val slot = row * GRID_SIZE + col
                val depth = depthOf(col, row)
                val scale = scaleOf(depth)
                val entity = occupancy[slot]
                val tree = entity?.let { toPlaced(it, row, col, depth, scale) }
                cells.add(
                    ForestGridCell(
                        row = row,
                        col = col,
                        depth = depth,
                        scale = scale,
                        tree = tree
                    )
                )
            }
        }

        return ForestSceneModel(
            cells = cells,
            trees = cells.mapNotNull { it.tree }
        )
    }

    /** Preferred slot from plant id; linear-probe until an free slot is found. */
    fun allocateSlot(plantId: String, occupied: Set<Int>): Int {
        val preferred = preferredSlot(plantId)
        for (offset in 0 until totalCells) {
            val slot = (preferred + offset) % totalCells
            if (slot !in occupied) return slot
        }
        return preferred
    }

    fun preferredSlot(plantId: String): Int {
        val h = mixPlantId(plantId)
        return (abs(h) % totalCells).toInt()
    }

    private fun toPlaced(
        entity: GardenPlantEntity,
        row: Int,
        col: Int,
        depth: Float,
        scale: Float
    ): PlacedTree = PlacedTree(
        plantId = entity.id,
        speciesId = entity.speciesId.ifBlank { GardenSpeciesCatalog.DEFAULT_SPECIES_ID },
        status = entity.status,
        moduleId = entity.moduleId,
        completedQuestionCount = entity.completedQuestionCount,
        studyDurationSec = entity.studyDurationSec,
        row = row,
        col = col,
        depth = depth,
        scale = scale
    )

    private fun mixPlantId(plantId: String): Long {
        var h = 0x9E3779B97F4A7C15uL.toLong()
        for (ch in plantId) {
            h = h * 31L + ch.code
        }
        h = h xor (h ushr 33)
        h *= -0xae502812aa7333L
        return h
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
