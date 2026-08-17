package com.example.seedie.domain.model

object DewConstants {
    const val DAILY_DEW_CAP = 200
    const val DAILY_CONVERT_TOKEN_MAX = 10
    const val TOKEN_TO_DEW_RATE = 10
    const val DEW_UNLOCK_PREMIUM_RATE = 1.2f

    const val CHECK_IN_STREAK_CAP_DAYS = 7

    fun dewForStreakDay(streakDay: Int): Int {
        return when {
            streakDay <= 0 -> 3
            streakDay <= 2 -> 3
            streakDay <= 4 -> 4
            else -> 5
        }.coerceAtLeast(3)
    }
}
