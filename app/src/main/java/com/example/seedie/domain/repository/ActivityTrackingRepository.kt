package com.example.seedie.domain.repository

import com.example.seedie.domain.model.ActivityModuleSummary
import com.example.seedie.domain.model.DailyActivityTotal
import kotlinx.coroutines.flow.Flow

interface ActivityTrackingRepository {
    suspend fun recordSegment(moduleId: String, startTimeMillis: Long, endTimeMillis: Long)

    fun observeTodayModuleSummaries(): Flow<List<ActivityModuleSummary>>

    fun observeRecentDailyTotals(days: Int): Flow<List<DailyActivityTotal>>
}
