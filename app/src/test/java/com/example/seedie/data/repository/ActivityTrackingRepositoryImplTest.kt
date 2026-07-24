package com.example.seedie.data.repository

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityTrackingRepositoryImplTest {

    @Test
    fun splitTrackedSegmentByDate_splitsSegmentAcrossMidnight() {
        val zoneId = ZoneId.of("Asia/Shanghai")
        val startTimeMillis = LocalDateTime.of(2026, 7, 1, 23, 59, 30)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
        val endTimeMillis = LocalDateTime.of(2026, 7, 2, 0, 1, 10)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        val result = splitTrackedSegmentByDate(
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            zoneId = zoneId
        )

        assertEquals(
            listOf(
                DatedActivitySegment(date = "2026-07-01", durationSec = 30),
                DatedActivitySegment(date = "2026-07-02", durationSec = 70)
            ),
            result
        )
    }

    @Test
    fun splitTrackedSegmentByDate_returnsEmptyWhenRangeIsInvalid() {
        val zoneId = ZoneId.of("UTC")

        assertEquals(
            emptyList<DatedActivitySegment>(),
            splitTrackedSegmentByDate(
                startTimeMillis = 1_000L,
                endTimeMillis = 1_000L,
                zoneId = zoneId
            )
        )
    }
}
