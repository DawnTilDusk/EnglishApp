package com.example.seedie.data.repository

import androidx.room.withTransaction
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.entity.DailyTaskEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.domain.model.StudyDay
import com.example.seedie.domain.model.StudyResult
import com.example.seedie.domain.model.completedDailyTaskKey
import com.example.seedie.domain.repository.DewManager
import com.example.seedie.domain.repository.EconomyManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DailyTaskManager @Inject constructor(
    private val database: SeedieDatabase,
    private val auth: AuthService,
    private val dews: DewManager,
    private val tokens: EconomyManager
) {
    private val mutex = Mutex()

    suspend fun ensureTasks(userId: String, date: String = StudyDay.date()) {
        database.withTransaction {
            listOf(
                Triple("daily_vocabulary", "完成 20 个单词的学习", 20),
                Triple("daily_listening", "完成一次听力训练", 1),
                Triple("daily_reading", "完成一次阅读训练", 1)
            ).forEach { (key, title, target) ->
                database.dailyTaskDao().insertIfAbsent(DailyTaskEntity(
                    userId = userId, date = date, title = title, rewardAmount = 8,
                    taskKey = key, targetCount = target
                ))
            }
        }
    }

    suspend fun ensureWritingTask(userId: String, date: String) {
        database.dailyTaskDao().insertIfAbsent(DailyTaskEntity(
            userId = userId, date = date, title = "提交一次作文", taskKey = "daily_writing",
            rewardType = "token", rewardAmount = 0, tokenReward = 10
        ))
    }

    suspend fun reconcileVocabulary(userId: String, date: String = StudyDay.date()) = mutex.withLock {
        ensureTasks(userId, date)
        val count = database.learningStateDao().wordCount(userId, date)
        database.dailyTaskDao().setProgress(userId, date, "daily_vocabulary", count)
        grantPending(userId, date)
        database.dailyTaskDao().pendingRewardDates(userId).filter { it != date }.forEach { grantPending(userId, it) }
    }

    suspend fun completeFromResult(userId: String, result: StudyResult) = mutex.withLock {
        if (!result.countsTowardTasks || !result.isCompleted || result.completedQuestionCount <= 0) return@withLock
        val date = StudyDay.date(result.completedAtMillis)
        ensureTasks(userId, date)
        val key = result.completedDailyTaskKey()
        if (key == "daily_writing") ensureWritingTask(userId, date)
        if (key != null) database.dailyTaskDao().setProgress(userId, date, key, 1)
        val legacyKey = key ?: if (result.moduleId == "vocabulary") "daily_vocabulary" else null
        if (legacyKey != null) database.dailyTaskDao().completeLegacy(userId, date, legacyKey, result.completedAtMillis)
        // Vocabulary is counted when each word passes its study stages, never by a session/quiz result.
        val count = database.learningStateDao().wordCount(userId, date)
        database.dailyTaskDao().setProgress(userId, date, "daily_vocabulary", count)
        grantPending(userId, date)
    }

    private suspend fun grantPending(userId: String, date: String) {
        for (task in database.dailyTaskDao().getTasksByUserAndDate(userId, date).first()) {
            if (!task.isCompleted || task.rewardGranted || !task.autoClaim) continue
            if (auth.currentSession.value?.userId != userId) return
            val ref = "task:$userId:$date:${task.taskKey}"
            if (task.rewardType == "dew") {
                dews.addDews(task.rewardAmount, "Task: ${task.title}", "dew:$ref")
            } else if (task.tokenReward > 0) {
                tokens.addTokens(task.tokenReward, "Completed: ${task.title}", ref)
            }
            if (auth.currentSession.value?.userId != userId) return
            database.dailyTaskDao().markRewardGranted(task.id)
        }
    }
}
