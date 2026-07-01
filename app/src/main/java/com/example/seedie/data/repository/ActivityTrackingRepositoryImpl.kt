package com.example.seedie.data.repository

import com.example.seedie.data.local.dao.ActivityDurationDao
import com.example.seedie.data.local.entity.ActivityDurationEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.ActivityModuleSummary
import com.example.seedie.domain.model.DailyActivityTotal
import com.example.seedie.domain.repository.ActivityTrackingRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ActivityTrackingRepositoryImpl @Inject constructor(
    private val activityDurationDao: ActivityDurationDao,
    private val authService: AuthService,
    private val ioDispatcher: CoroutineDispatcher
) : ActivityTrackingRepository {

    private val clock: Clock = Clock.systemDefaultZone()
    private val zoneId: ZoneId = clock.zone

    override suspend fun recordSegment(
        moduleId: String,
        startTimeMillis: Long,
        endTimeMillis: Long
    ) {
        if (moduleId.isBlank() || endTimeMillis <= startTimeMillis) return
        val userId = authService.currentSession.value?.userId ?: return

        withContext(ioDispatcher) {
            splitTrackedSegmentByDate(
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                zoneId = zoneId
            ).forEach { segment ->
                if (segment.durationSec <= 0) return@forEach
                val updatedCount = activityDurationDao.incrementDuration(
                    userId = userId,
                    date = segment.date,
                    moduleId = moduleId,
                    durationSec = segment.durationSec,
                    updatedAt = endTimeMillis
                )
                if (updatedCount == 0) {
                    val insertResult = activityDurationDao.insert(
                        ActivityDurationEntity(
                            userId = userId,
                            date = segment.date,
                            moduleId = moduleId,
                            durationSec = segment.durationSec,
                            updatedAt = endTimeMillis
                        )
                    )
                    if (insertResult == -1L) {
                        activityDurationDao.incrementDuration(
                            userId = userId,
                            date = segment.date,
                            moduleId = moduleId,
                            durationSec = segment.durationSec,
                            updatedAt = endTimeMillis
                        )
                    }
                }
            }
        }
    }

    override fun observeTodayModuleSummaries(): Flow<List<ActivityModuleSummary>> {
        val userId = authService.currentSession.value?.userId ?: return flowOf(emptyList())
        val today = LocalDate.now(clock).toString()
        return activityDurationDao.observeModuleSummariesByDate(userId, today)
            .map { rows ->
                rows.map { row ->
                    ActivityModuleSummary(
                        moduleId = row.moduleId,
                        durationSec = row.durationSec
                    )
                }
            }
    }

    override fun observeRecentDailyTotals(days: Int): Flow<List<DailyActivityTotal>> {
        if (days <= 0) return flowOf(emptyList())
        val userId = authService.currentSession.value?.userId ?: return flowOf(emptyList())
        val endDate = LocalDate.now(clock)
        val startDate = endDate.minusDays((days - 1).toLong())
        return activityDurationDao.observeDailyTotalsBetween(
            userId = userId,
            startDate = startDate.toString(),
            endDate = endDate.toString()
        ).map { rows ->
            rows.map { row ->
                DailyActivityTotal(
                    date = row.date,
                    durationSec = row.durationSec
                )
            }
        }
    }
}

internal data class DatedActivitySegment(
    val date: String,
    val durationSec: Int
)

internal fun splitTrackedSegmentByDate(
    startTimeMillis: Long,
    endTimeMillis: Long,
    zoneId: ZoneId
): List<DatedActivitySegment> {
    if (endTimeMillis <= startTimeMillis) return emptyList()

    val segments = mutableListOf<DatedActivitySegment>()
    var cursor = startTimeMillis
    while (cursor < endTimeMillis) {
        val currentDate = Instant.ofEpochMilli(cursor).atZone(zoneId).toLocalDate()
        val nextBoundaryMillis = currentDate
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val segmentEnd = minOf(endTimeMillis, nextBoundaryMillis)
        val durationSec = ((segmentEnd - cursor) / 1000L).toInt()
        if (durationSec > 0) {
            segments += DatedActivitySegment(
                date = currentDate.toString(),
                durationSec = durationSec
            )
        }
        cursor = segmentEnd
    }
    return segments
}
