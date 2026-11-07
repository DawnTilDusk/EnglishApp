package com.example.seedie.domain.model

fun StudyResult.completedDailyTaskKey(): String? {
    if (!countsTowardTasks || !isCompleted || completedQuestionCount <= 0) return null
    return when (moduleId) {
        "reading" -> "daily_reading"
        "listening" -> "daily_listening"
        "writing" -> "daily_writing"
        else -> null // Word learning counts individual completed study items, never quiz/question totals.
    }
}

fun StudyResult.tokenRewardRef(): String = when (moduleId) {
    "reading", "listening", "writing" -> if (sessionId.startsWith("free_")) sessionId else "assignment:$sessionId"
    else -> "study:$sessionId"
}

fun StudyResult.dewRewardRef(): String = "dew:study:$sessionId"
