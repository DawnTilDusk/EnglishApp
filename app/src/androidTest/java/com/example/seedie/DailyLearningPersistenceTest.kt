package com.example.seedie

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.SeedieDatabaseMigrations
import com.example.seedie.data.local.entity.*
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class DailyLearningPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun dailyWordCountsSurviveRepetitionReviewAndAccountChanges() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, SeedieDatabase::class.java).build()
        try {
            val words = db.learningStateDao()
            val tasks = db.dailyTaskDao()
            tasks.insertIfAbsent(DailyTaskEntity(userId = "student", date = "2026-11-06",
                title = "完成 20 个单词的学习", taskKey = "daily_vocabulary", targetCount = 20))
            repeat(8) { words.recordWord(DailyWordCompletionEntity("student", "2026-11-06", "book", "w$it", 1)) }
            tasks.setProgress("student", "2026-11-06", "daily_vocabulary", words.wordCount("student", "2026-11-06"))
            assertFalse(tasks.getTasksByUserAndDate("student", "2026-11-06").first().single().isCompleted)
            repeat(20) { words.recordWord(DailyWordCompletionEntity("student", "2026-11-06", "book", "w$it", 2)) }
            tasks.setProgress("student", "2026-11-06", "daily_vocabulary", words.wordCount("student", "2026-11-06"))
            val done = tasks.getTasksByUserAndDate("student", "2026-11-06").first().single()
            assertEquals(20, done.currentCount)
            assertTrue(done.isCompleted)
            assertFalse(done.rewardGranted)
            assertEquals(listOf("2026-11-06"), tasks.pendingRewardDates("student"))
            assertEquals(0, words.wordCount("other-student", "2026-11-06"))
            assertEquals(0, words.wordCount("student", "2026-11-07"))
            tasks.setProgress("student", "2026-11-06", "daily_vocabulary", 1)
            assertTrue(tasks.getTasksByUserAndDate("student", "2026-11-06").first().single().isCompleted)
            tasks.markRewardGranted(done.id)
            assertTrue(tasks.pendingRewardDates("student").isEmpty())
        } finally { db.close() }
    }

    @Test fun durableOutboxIgnoresDuplicateResultsAndKeepsOtherUsersIsolated() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, SeedieDatabase::class.java).build()
        try {
            val dao = db.learningStateDao()
            val first = StudySettlementEntity("student", "reading:attempt", "original", 1)
            dao.enqueue(first)
            dao.enqueue(first.copy(payload = "retry", createdAt = 2))
            assertEquals(listOf(first), dao.pending("student"))
            assertTrue(dao.pending("other").isEmpty())
            dao.markSettled("student", first.settlementId)
            dao.enqueue(first)
            assertTrue(dao.pending("student").isEmpty())
            dao.saveDraft(PracticeDraftEntity("student", "reading:item", "answers", 1))
            assertNull(dao.draft("other", "reading:item"))
            assertEquals("answers", dao.draft("student", "reading:item")?.payload)
        } finally { db.close() }
    }

    @Test fun migrationPreservesHistoricalTasksWithoutAwardingThemAgain() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null).callback(object : SupportSQLiteOpenHelper.Callback(12) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""CREATE TABLE daily_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL, date TEXT NOT NULL, title TEXT NOT NULL, isCompleted INTEGER NOT NULL,
                        rewardType TEXT NOT NULL, rewardAmount INTEGER NOT NULL, tokenReward INTEGER NOT NULL,
                        autoClaim INTEGER NOT NULL, completedAt INTEGER, taskKey TEXT NOT NULL)""")
                    db.execSQL("INSERT INTO daily_tasks VALUES (1,'student','2026-11-05','old task',1,'dew',8,0,1,1,'daily_vocabulary')")
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            }).build())
        try {
            val db = helper.writableDatabase
            SeedieDatabaseMigrations.MIGRATION_12_13.migrate(db)
            db.query("SELECT title, isCompleted, ruleVersion, rewardGranted FROM daily_tasks").use {
                assertTrue(it.moveToFirst())
                assertEquals("old task", it.getString(0))
                assertEquals(1, it.getInt(1)); assertEquals(1, it.getInt(2)); assertEquals(1, it.getInt(3))
            }
            db.query("SELECT COUNT(*) FROM study_settlements").use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
        } finally { helper.close() }
    }
}
