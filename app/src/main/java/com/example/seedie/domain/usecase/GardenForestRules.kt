package com.example.seedie.domain.usecase

import com.example.seedie.data.local.entity.GardenPlantEntity

object GardenForestRules {
    const val ABANDON_GRACE_MS: Long = 60_000L
    const val REMOVE_WITHERED_COST: Int = 50

    /**
     * @param abandonElapsedMs wall-clock since session open; when [sessionOpenedAtMillis] was 0,
     * callers should pass [ABANDON_GRACE_MS] (or greater) so grace does not apply.
     * @return plant status, or null to skip recording
     */
    fun statusFor(
        isCompleted: Boolean,
        questionCount: Int,
        abandonElapsedMs: Long
    ): String? {
        return when {
            isCompleted && questionCount > 0 -> GardenPlantEntity.STATUS_ALIVE
            isCompleted -> null
            abandonElapsedMs < ABANDON_GRACE_MS -> null
            else -> GardenPlantEntity.STATUS_WITHERED
        }
    }

    fun isWithinAbandonGrace(
        sessionOpenedAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (sessionOpenedAtMillis <= 0L) return false
        return nowMillis - sessionOpenedAtMillis < ABANDON_GRACE_MS
    }

    fun abandonElapsedMs(
        sessionOpenedAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        if (sessionOpenedAtMillis <= 0L) return ABANDON_GRACE_MS
        return (nowMillis - sessionOpenedAtMillis).coerceAtLeast(0L)
    }
}
