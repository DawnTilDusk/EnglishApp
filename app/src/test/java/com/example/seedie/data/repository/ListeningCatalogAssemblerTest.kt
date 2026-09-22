package com.example.seedie.data.repository

import com.example.seedie.data.remote.SupabaseListeningMaterial
import com.example.seedie.data.remote.SupabaseListeningQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningCatalogAssemblerTest {
    @Test
    fun includesNewPracticePacksButExcludesTextbooksAndMaterialsWithoutQuestions() {
        val materials = listOf(
            SupabaseListeningMaterial("new", title = "New", title_zh = "新材料", grade_level = 15, sort_order = 2),
            SupabaseListeningMaterial("l08-01", title = "Original", sort_order = 1),
            SupabaseListeningMaterial("textbook", book_id = "pep-g7-vol1"),
            SupabaseListeningMaterial("unfinished")
        )
        val questions = listOf("new", "l08-01", "textbook").map {
            SupabaseListeningQuestion(question_id = "$it-q1", material_id = it)
        }
        val items = ListeningCatalogAssembler.assemble(materials, questions, setOf("new"))
        assertEquals(listOf("l08-01", "new"), items.map { it.itemRef })
        assertEquals("新材料", items[1].title)
        assertTrue(items[1].subtitle.orEmpty().contains("八年级"))
        assertTrue(items[1].completed)
        assertFalse(items[0].completed)
    }

    @Test
    fun missingChineseTitleFallsBackToEnglishWithoutRepeatingItInSubtitle() {
        val items = ListeningCatalogAssembler.assemble(
            listOf(SupabaseListeningMaterial("new", title = "A Short Talk", title_zh = "", estimated_seconds = 40)),
            listOf(SupabaseListeningQuestion("q1", "new")),
            emptySet()
        )
        assertEquals("A Short Talk", items.single().title)
        assertEquals("约 40 秒", items.single().subtitle)
    }
}
