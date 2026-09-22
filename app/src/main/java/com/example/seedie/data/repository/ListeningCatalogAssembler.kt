package com.example.seedie.data.repository

import com.example.seedie.data.remote.SupabaseListeningMaterial
import com.example.seedie.data.remote.SupabaseListeningQuestion
import com.example.seedie.domain.model.PracticeCatalogItem

object ListeningCatalogAssembler {
    fun assemble(
        materials: List<SupabaseListeningMaterial>,
        questions: List<SupabaseListeningQuestion>,
        completed: Set<String>
    ): List<PracticeCatalogItem> {
        val materialIdsWithQuestions = questions.map { it.material_id }.toSet()
        return materials
            .filter { it.book_id.isNullOrBlank() && it.material_id in materialIdsWithQuestions }
            .sortedBy { it.sort_order }
            .map { material ->
                val title = material.title_zh?.takeIf { it.isNotBlank() }
                    ?: material.title?.takeIf { it.isNotBlank() }
                    ?: "听力练习"
                PracticeCatalogItem(
                    itemRef = material.material_id,
                    title = title,
                    subtitle = buildList {
                        when (material.grade_level) {
                            13, 14 -> add("七年级")
                            15, 16 -> add("八年级")
                            17, 18 -> add("九年级")
                        }
                        material.title?.takeIf { it.isNotBlank() && it != title }?.let { add(it) }
                        if (material.estimated_seconds > 0) add("约 ${material.estimated_seconds} 秒")
                    }.joinToString(" · ").ifBlank { null },
                    completed = material.material_id in completed
                )
            }
    }
}
