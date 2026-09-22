package com.example.seedie.ui.screens.learning.listening.immersion

import com.example.seedie.data.remote.SupabaseListeningMaterial
import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningImmersionTest {
    @Test
    fun catalogExcludesTextbooksPlaceholderAndUnplayableMaterials() {
        val sample = SupabaseListeningMaterial(material_id = "original-a", transcript = "A short story.",
            audio_url = "https://example.com/story.mp3", sort_order = 2)
        val result = immersionMaterials(listOf(
            sample,
            sample.copy(material_id = "textbook", book_id = "book"),
            sample.copy(material_id = "l08-01"),
            sample.copy(material_id = "no-audio", audio_url = null),
            sample.copy(material_id = "no-text", transcript = " "),
            sample.copy(material_id = "insecure", audio_url = "http://example.com/story.mp3"),
            sample.copy(material_id = "original-b", sort_order = 1),
            sample
        ))
        assertEquals(listOf("original-b", "original-a"), result.map { it.material_id })
    }

    @Test
    fun resumeRestartsFinishedClipButKeepsPartiallyListenedPosition() {
        assertEquals(0L, resumePosition(30_000, 30_000))
        assertEquals(0L, resumePosition(45_000, 30_000))
        assertEquals(12_000L, resumePosition(12_000, 30_000))
    }

    @Test
    fun resumeHandlesUnknownDurationAndInvalidStoredPosition() {
        assertEquals(12_000L, resumePosition(12_000, -1))
        assertEquals(0L, resumePosition(-100, 30_000))
    }
}
