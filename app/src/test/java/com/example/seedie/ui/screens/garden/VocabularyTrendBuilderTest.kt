package com.example.seedie.ui.screens.garden

import com.example.seedie.domain.repository.VocabularyEstimateRecord
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VocabularyTrendBuilderTest {

    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val dateRange = VocabularyTrendDateRange(
        startDate = LocalDate.of(2026, 8, 1),
        endDateInclusive = LocalDate.of(2026, 8, 5)
    )

    @Test
    fun buildVocabularyTrendPoints_usesHighestMeasurementForTheSameDay() {
        val points = buildVocabularyTrendPoints(
            records = listOf(
                estimate("first", 120, "2026-08-01T01:00:00Z"),
                estimate("highest", 135, "2026-08-01T08:00:00Z"),
                estimate("last", 165, "2026-08-05T08:00:00Z")
            ),
            range = VocabularyTrendRange.Last7Days,
            dateRange = dateRange,
            zoneId = zoneId
        )

        val firstDay = points.first()
        assertEquals(LocalDate.of(2026, 8, 1), firstDay.date)
        assertEquals(135, firstDay.value)
        assertEquals(VocabularyTrendPointSource.ActualMeasurement, firstDay.source)
        assertEquals(2, firstDay.actualMeasurementCount)
        assertEquals(165, points.last().value)
    }

    @Test
    fun buildVocabularyTrendPoints_interpolatesDeterministicallyBetweenActualAnchors() {
        val records = listOf(
            estimate("start", 100, "2026-08-01T08:00:00Z"),
            estimate("end", 140, "2026-08-05T08:00:00Z")
        )

        val first = buildVocabularyTrendPoints(
            records = records,
            range = VocabularyTrendRange.Last7Days,
            dateRange = dateRange,
            zoneId = zoneId
        )
        val second = buildVocabularyTrendPoints(
            records = records,
            range = VocabularyTrendRange.Last7Days,
            dateRange = dateRange,
            zoneId = zoneId
        )

        assertEquals(first, second)
        assertEquals(VocabularyTrendPointSource.ActualMeasurement, first[0].source)
        assertEquals(VocabularyTrendPointSource.Interpolated, first[1].source)
        assertEquals(VocabularyTrendPointSource.ActualMeasurement, first[4].source)
        assertTrue(first[1].value in 100..140)
        assertTrue(first[2].value in 100..140)
        assertTrue(first[3].value in 100..140)
    }

    @Test
    fun buildVocabularyTrendPoints_doesNotGuessBeforeFirstMeasurementAndCarriesLatestForward() {
        val points = buildVocabularyTrendPoints(
            records = listOf(estimate("only", 180, "2026-08-03T08:00:00Z")),
            range = VocabularyTrendRange.Last7Days,
            dateRange = dateRange,
            zoneId = zoneId
        )

        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 4),
                LocalDate.of(2026, 8, 5)
            ),
            points.map { it.date }
        )
        assertEquals(VocabularyTrendPointSource.ActualMeasurement, points[0].source)
        assertEquals(VocabularyTrendPointSource.CarriedForward, points[1].source)
        assertEquals(VocabularyTrendPointSource.CarriedForward, points[2].source)
        assertTrue(points.drop(1).all { it.value == 180 })
    }

    @Test
    fun buildVocabularyTrendPoints_changeMetricUsesOnlyActualMeasurements() {
        val points = buildVocabularyTrendPoints(
            records = listOf(
                estimate("previous", 100, "2026-07-31T08:00:00Z"),
                estimate("first", 120, "2026-08-02T08:00:00Z"),
                estimate("second", 110, "2026-08-05T08:00:00Z")
            ),
            range = VocabularyTrendRange.Last7Days,
            metric = VocabularyTrendMetric.MeasurementChange,
            dateRange = dateRange,
            zoneId = zoneId
        )

        assertEquals(listOf(20, -10), points.map { it.value })
        assertTrue(points.all { it.source == VocabularyTrendPointSource.ActualMeasurement })
        assertFalse(points.any { it.source == VocabularyTrendPointSource.Interpolated })
    }

    @Test
    fun resolveDateRange_usesCalendarMonthAndYearBoundaries() {
        val today = LocalDate.of(2026, 8, 17)

        assertEquals(
            LocalDate.of(2026, 8, 1),
            VocabularyTrendRange.ThisMonth.resolveDateRange(today).startDate
        )
        assertEquals(
            LocalDate.of(2026, 1, 1),
            VocabularyTrendRange.ThisYear.resolveDateRange(today).startDate
        )
        assertEquals(
            LocalDate.of(2025, 8, 18),
            VocabularyTrendRange.LastYear.resolveDateRange(today).startDate
        )
    }

    @Test
    fun buildVocabularyTrendPoints_samplesYearRangesAtTheLastAvailableDayOfEachMonth() {
        val points = buildVocabularyTrendPoints(
            records = listOf(
                estimate("jan", 100, "2026-01-10T08:00:00Z"),
                estimate("mar", 160, "2026-03-10T08:00:00Z")
            ),
            range = VocabularyTrendRange.ThisYear,
            dateRange = VocabularyTrendDateRange(
                startDate = LocalDate.of(2026, 1, 1),
                endDateInclusive = LocalDate.of(2026, 3, 31)
            ),
            zoneId = zoneId
        )

        assertEquals(listOf("1月", "2月", "3月"), points.map { it.shortLabel })
        assertEquals(LocalDate.of(2026, 3, 31), points.last().date)
        assertEquals(VocabularyTrendPointSource.CarriedForward, points.last().source)
    }

    @Test
    fun buildVocabularyTrendPoints_acceptsPostgresTimestampWithSpaceAndCompactOffset() {
        val points = buildVocabularyTrendPoints(
            records = listOf(
                estimate("postgres-format", 240, "2026-08-17 10:42:50.301407+0000")
            ),
            range = VocabularyTrendRange.Last7Days,
            dateRange = VocabularyTrendDateRange(
                startDate = LocalDate.of(2026, 8, 11),
                endDateInclusive = LocalDate.of(2026, 8, 17)
            ),
            zoneId = zoneId
        )

        assertEquals(1, points.size)
        assertEquals(LocalDate.of(2026, 8, 17), points.single().date)
        assertEquals(VocabularyTrendPointSource.ActualMeasurement, points.single().source)
    }

    @Test
    fun buildVocabularyTrendPoints_acceptsSupabaseUtcOffsetTimestamp() {
        val points = buildVocabularyTrendPoints(
            records = listOf(
                estimate("supabase-format", 240, "2026-08-17T10:42:50.301407+00:00")
            ),
            range = VocabularyTrendRange.Last7Days,
            dateRange = VocabularyTrendDateRange(
                startDate = LocalDate.of(2026, 8, 11),
                endDateInclusive = LocalDate.of(2026, 8, 17)
            ),
            zoneId = zoneId
        )

        assertEquals(1, points.size)
        assertEquals(LocalDate.of(2026, 8, 17), points.single().date)
        assertEquals(VocabularyTrendPointSource.ActualMeasurement, points.single().source)
    }

    @Test
    fun trendXAxisLabelIndices_evenlySamplesThirtyDaysIncludingBothEnds() {
        assertEquals(
            listOf(0, 6, 12, 17, 23, 29),
            trendXAxisLabelIndices(30)
        )
    }

    @Test
    fun buildTrendAxisScale_usesReadableVocabularyTicksWithBreathingRoom() {
        val scale = buildTrendAxisScale(
            values = listOf(720, 874, 1080),
            metric = VocabularyTrendMetric.Estimate
        )

        assertEquals(600, scale.minimum)
        assertEquals(1200, scale.maximum)
        assertEquals(listOf(600, 800, 1000, 1200), scale.ticks)
    }

    @Test
    fun buildTrendAxisScale_centersMixedMeasurementChangesOnZero() {
        val scale = buildTrendAxisScale(
            values = listOf(20, -10, 0),
            metric = VocabularyTrendMetric.MeasurementChange
        )

        assertEquals(-20, scale.minimum)
        assertEquals(20, scale.maximum)
        assertEquals(listOf(-20, -10, 0, 10, 20), scale.ticks)
    }

    @Test
    fun coerceListIndex_returnsNullForEmptyAndClampsOtherwise() {
        assertEquals(null, coerceListIndex(0, 0))
        assertEquals(0, coerceListIndex(3, -1))
        assertEquals(2, coerceListIndex(3, 99))
        assertEquals(1, coerceListIndex(3, 1))
    }

    private fun estimate(id: String, size: Int, createdAt: String): VocabularyEstimateRecord {
        return VocabularyEstimateRecord(id = id, vocabularySize = size, createdAt = createdAt)
    }
}
