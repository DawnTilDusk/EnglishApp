package com.example.seedie

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.example.seedie.data.local.DevicePreferencesRepository
import com.example.seedie.data.local.SeedieDatabase
import com.example.seedie.data.local.entity.VocabularyWordLearningProgressEntity
import com.example.seedie.data.remote.AuthService
import com.example.seedie.data.repository.VocabularyOptionBuilder
import com.example.seedie.data.repository.VocabularyPracticeRepositoryImpl
import com.example.seedie.data.repository.WordBookSeeder
import com.example.seedie.data.sync.SyncManager
import com.example.seedie.data.sync.SyncResult
import com.example.seedie.data.sync.SyncScope
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeArgs
import com.example.seedie.ui.screens.learning.practice.VocabularyPracticeMode
import com.example.seedie.ui.screens.learning.practice.VocabularyResumeWordProgress
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class VocabularyReviewProgressTest {
    @Test
    fun partialStudyCanReviewAndFinishWithoutRequeueingReviewedWords() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, SeedieDatabase::class.java).build()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val client = createSupabaseClient("https://example.invalid", "test-key") {
            install(Auth) {
                autoLoadFromStorage = false
                alwaysAutoRefresh = false
            }
        }
        try {
            val repository = VocabularyPracticeRepositoryImpl(
                database,
                database.wordBookDao(),
                database.vocabularyWordDao(),
                database.vocabularyBookProgressDao(),
                database.vocabularyWordLearningProgressDao(),
                database.vocabularyStudyRoundDao(),
                database.vocabularyStudyRoundWordDao(),
                AuthService(client, DevicePreferencesRepository(context)),
                object : SyncManager {
                    override suspend fun syncNow(scope: SyncScope) = SyncResult.Success
                },
                WordBookSeeder(database.wordBookDao(), database.vocabularyWordDao()),
                VocabularyOptionBuilder(),
                scope
            )
            val args = VocabularyPracticeArgs(difficulty = "easy")
            val study = repository.getPracticeSession(args)
            val initial = requireNotNull(study.resumeSnapshot)
            assertEquals(8, initial.studyTargetCount)
            val learnedIds = initial.activeWordIds.take(2)
            val activeIds = initial.activeWordIds.drop(2)
            val partial = initial.copy(
                activeWordIds = activeIds,
                masteredStudyCount = 2,
                wordProgressList = activeIds.map { VocabularyResumeWordProgress(it) }
            )
            repository.saveStudyRoundSnapshot(partial)
            // 兼容旧版本留下的 LEARNED 状态。
            database.vocabularyWordLearningProgressDao().insertOrReplace(
                VocabularyWordLearningProgressEntity(
                    "", initial.bookId, learnedIds.first(), "LEARNED", 1L, 1L
                )
            )
            val pending = requireNotNull(repository.getPendingReviewEntry())
            assertEquals(2, pending.pendingWordCount)
            val review = repository.getPracticeSession(
                VocabularyPracticeArgs(
                    entryMode = VocabularyPracticeMode.Review,
                    sourceModuleId = "vocabulary_review",
                    targetRoundId = pending.roundId
                )
            )
            val reviewRound = requireNotNull(review.reviewRoundId)
            assertNotNull(database.vocabularyStudyRoundDao().getRoundById(reviewRound))
            assertEquals(learnedIds.toSet(), review.reviewWords.map { it.wordId }.toSet())
            repository.markReviewWordMastered(reviewRound, learnedIds.first())
            assertEquals(1, repository.getPendingReviewEntry()?.pendingWordCount)
            repository.markReviewWordSentBackToLearning(reviewRound, learnedIds.last())
            assertEquals(null, repository.getPendingReviewEntry())

            val resumed = repository.getPracticeSession(args)
            assertEquals(2, resumed.resumeSnapshot?.masteredStudyCount)
            assertEquals(8, resumed.resumeSnapshot?.studyTargetCount)
            val remainingIds = resumed.studyWords.map { it.wordId }
            repository.saveStudyRoundSnapshot(partial.copy(
                activeWordIds = remainingIds,
                introducedStudyCount = 8,
                nextWordSortOrderCursor = Int.MAX_VALUE,
                wordProgressList = remainingIds.map { VocabularyResumeWordProgress(it) }
            ))
            repository.saveStudyRoundSnapshot(partial.copy(
                activeWordIds = emptyList(),
                introducedStudyCount = 8,
                masteredStudyCount = 8,
                nextWordSortOrderCursor = Int.MAX_VALUE,
                wordProgressList = emptyList()
            ))
            repository.markStudyRoundReviewPending(initial.roundId)
            assertEquals(6, repository.getPendingReviewEntry()?.pendingWordCount)
            assertEquals(6, repository.getPendingReviewWordCount(initial.roundId))
            val nextStudy = repository.getPracticeSession(args)
            assertEquals(listOf(learnedIds.last()), nextStudy.studyWords.map { it.wordId })
        } finally {
            scope.cancel()
            client.close()
            database.close()
        }
    }
}
