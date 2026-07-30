package com.example.seedie.data.repository

import com.example.seedie.data.remote.SupabaseListeningMaterial
import com.example.seedie.data.remote.SupabaseListeningOption
import com.example.seedie.data.remote.SupabaseListeningQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningSessionAssemblerTest {

    @Test
    fun assemble_ordersMaterialsQuestionsAndOptions() {
        val session = ListeningSessionAssembler.assemble(
            sessionId = "s1",
            materials = listOf(
                material(id = "m2", sort = 1, title = "Second"),
                material(id = "m1", sort = 0, title = "First")
            ),
            questions = listOf(
                question(id = "q2", materialId = "m1", sort = 1, correct = "B"),
                question(id = "q1", materialId = "m1", sort = 0, correct = "A")
            ),
            options = listOf(
                option("q1", "B", "Wrong", 1),
                option("q1", "A", "Right", 0),
                option("q2", "B", "Right", 0),
                option("q2", "A", "Wrong", 1)
            )
        )

        assertEquals("s1", session.sessionId)
        assertEquals(listOf("m1", "m2"), session.materials.map { it.materialId })
        assertEquals(listOf("q1", "q2"), session.materials.first().questions.map { it.questionId })
        assertEquals("A", session.materials.first().questions.first().options.first().optionId)
        assertTrue(session.materials.first().questions.first().options.first().isCorrect)
    }

    @Test
    fun assemble_keepsNullableRemoteTextCompatible() {
        val session = ListeningSessionAssembler.assemble(
            sessionId = "s2",
            materials = listOf(
                material(id = "m1", sort = 0, title = null)
            ),
            questions = listOf(
                question(id = "q1", materialId = "m1", sort = 0, correct = "A", stem = null)
            ),
            options = listOf(
                option("q1", "A", null, 0),
                option("q1", "B", "Other", 1)
            )
        )

        val material = session.materials.first()
        val question = material.questions.first()
        assertEquals("Listening Material", material.title)
        assertNull(material.audioUrl)
        assertEquals("", question.stem)
        assertEquals("A. ", question.options.first().label)
    }

    private fun material(
        id: String,
        sort: Int,
        title: String?
    ) = SupabaseListeningMaterial(
        material_id = id,
        title = title,
        title_zh = null,
        material_type = "dialogue",
        prompt_text = null,
        transcript = null,
        audio_url = null,
        estimated_seconds = 60,
        sort_order = sort
    )

    private fun question(
        id: String,
        materialId: String,
        sort: Int,
        correct: String,
        stem: String? = "Stem"
    ) = SupabaseListeningQuestion(
        question_id = id,
        material_id = materialId,
        question_type = "detail",
        stem = stem,
        sort_order = sort,
        correct_option_id = correct,
        explanation = "Because",
        reward_token = 2
    )

    private fun option(
        questionId: String,
        optionId: String,
        text: String?,
        sort: Int
    ) = SupabaseListeningOption(
        question_id = questionId,
        option_id = optionId,
        option_text = text,
        sort_order = sort
    )
}
