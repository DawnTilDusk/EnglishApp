package com.example.seedie.domain.model

data class GardenSpecies(
    val id: String,
    val displayName: String,
    val unlockCost: Int,
    val unlockedByDefault: Boolean,
    val accentColorArgb: Long
)

object GardenSpeciesCatalog {
    const val DEFAULT_SPECIES_ID = "default_sprout"

    val all: List<GardenSpecies> = listOf(
        GardenSpecies(
            id = DEFAULT_SPECIES_ID,
            displayName = "嫩芽",
            unlockCost = 0,
            unlockedByDefault = true,
            accentColorArgb = 0xFF7CB342
        ),
        GardenSpecies(
            id = "sunny_flower",
            displayName = "向日葵",
            unlockCost = 50,
            unlockedByDefault = false,
            accentColorArgb = 0xFFF9A825
        ),
        GardenSpecies(
            id = "cedar_tree",
            displayName = "雪松",
            unlockCost = 120,
            unlockedByDefault = false,
            accentColorArgb = 0xFF2E7D32
        ),
        GardenSpecies(
            id = "sakura",
            displayName = "樱花",
            unlockCost = 200,
            unlockedByDefault = false,
            accentColorArgb = 0xFFF48FB1
        ),
        GardenSpecies(
            id = "bamboo",
            displayName = "青竹",
            unlockCost = 80,
            unlockedByDefault = false,
            accentColorArgb = 0xFF66BB6A
        )
    )

    fun byId(id: String): GardenSpecies? = all.firstOrNull { it.id == id }

    fun requireById(id: String): GardenSpecies =
        byId(id) ?: byId(DEFAULT_SPECIES_ID)!!
}
