package com.example.seedie.domain.reading

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingGradeBandsTest {

    @Test
    fun bandsForProfileGrade_mapsJuniorYearsToBothVolumes() {
        assertEquals(setOf("g7a", "g7b"), ReadingGradeBands.bandsForProfileGrade("初一"))
        assertEquals(setOf("g8a", "g8b"), ReadingGradeBands.bandsForProfileGrade("初二"))
        assertEquals(setOf("g9a", "g9b"), ReadingGradeBands.bandsForProfileGrade("初三"))
    }

    @Test
    fun bandsForProfileGrade_trimsWhitespace() {
        assertEquals(setOf("g8a", "g8b"), ReadingGradeBands.bandsForProfileGrade(" 初二 "))
    }

    @Test
    fun bandsForProfileGrade_nonJuniorIsEmpty() {
        listOf("一年级", "六年级", "高一", "高三", "其他", null, "", "  ").forEach { grade ->
            assertTrue(
                "expected empty bands for grade=$grade",
                ReadingGradeBands.bandsForProfileGrade(grade).isEmpty()
            )
        }
    }

    @Test
    fun filterCatalog_keepsOnlyAllowedBands() {
        val bands = ReadingGradeBands.bandsForProfileGrade("初二")
        val setBands = listOf("g7a", "g8a", "g8b", "g9a", "g8a")
        val filtered = setBands.filter { it in bands }
        assertEquals(listOf("g8a", "g8b", "g8a"), filtered)
    }

    @Test
    fun displayName_returnsChineseVolumeLabel() {
        assertEquals("八上", ReadingGradeBands.displayName("g8a"))
        assertEquals(null, ReadingGradeBands.displayName("unknown"))
        assertEquals(null, ReadingGradeBands.displayName(null))
    }
}
