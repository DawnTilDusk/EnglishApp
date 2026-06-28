package com.example.seedie.data.sync.syncer

import com.example.seedie.data.local.dao.VocabularyBookProgressDao
import com.example.seedie.data.local.dao.VocabularyStudyRoundDao
import com.example.seedie.data.local.dao.VocabularyWordLearningProgressDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VocabularyProgressSyncer @Inject constructor(
    private val wordLearningProgressDao: VocabularyWordLearningProgressDao,
    private val studyRoundDao: VocabularyStudyRoundDao,
    private val bookProgressDao: VocabularyBookProgressDao,
    private val client: SupabaseClient
) {
    suspend fun sync(userId: String) {
        syncWordLearningProgress(userId)
        syncStudyRounds(userId)
        syncBookProgress(userId)
    }

    private suspend fun syncWordLearningProgress(userId: String) {
        val pending = wordLearningProgressDao.getPendingProgress(userId)
        if (pending.isEmpty()) return
        pending.forEach { item ->
            try {
                client.postgrest["user_vocabulary_word_learning_progress"].upsert(
                    UserVocabularyWordLearningProgressDto(
                        user_id = userId,
                        book_id = item.bookId,
                        word_id = item.wordId,
                        status = item.status,
                        last_studied_at = item.lastStudiedAt,
                        learned_at = item.learnedAt
                    )
                )
                wordLearningProgressDao.updateSyncStatus(
                    userId = userId,
                    bookId = item.bookId,
                    wordId = item.wordId,
                    status = "SYNCED",
                    syncedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                android.util.Log.e("VocabularyProgressSyncer", "Failed to sync word progress ${item.wordId}", e)
            }
        }
    }

    private suspend fun syncStudyRounds(userId: String) {
        val pending = studyRoundDao.getPendingRounds(userId)
        if (pending.isEmpty()) return
        pending.forEach { round ->
            try {
                client.postgrest["user_vocabulary_study_rounds"].upsert(
                    UserVocabularyStudyRoundDto(
                        round_id = round.roundId,
                        user_id = userId,
                        book_id = round.bookId,
                        status = round.status,
                        target_word_count = round.targetWordCount,
                        introduced_word_count = round.introducedWordCount,
                        mastered_word_count = round.masteredWordCount,
                        active_queue_size = round.activeQueueSize,
                        next_word_sort_order_cursor = round.nextWordSortOrderCursor,
                        created_at = round.createdAt,
                        updated_at = round.updatedAt
                    )
                )
                studyRoundDao.updateSyncStatus(
                    roundId = round.roundId,
                    status = "SYNCED",
                    syncedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                android.util.Log.e("VocabularyProgressSyncer", "Failed to sync study round ${round.roundId}", e)
            }
        }
    }

    private suspend fun syncBookProgress(userId: String) {
        val pending = bookProgressDao.getPendingProgress(userId)
        if (pending.isEmpty()) return
        pending.forEach { progress ->
            try {
                client.postgrest["user_vocabulary_book_progress"].upsert(
                    UserVocabularyBookProgressDto(
                        user_id = userId,
                        book_id = progress.bookId,
                        next_word_sort_order_cursor = progress.nextWordSortOrderCursor,
                        active_round_id = progress.activeRoundId,
                        learned_word_count = progress.learnedWordCount,
                        updated_at = progress.updatedAt
                    )
                )
                bookProgressDao.updateSyncStatus(
                    userId = userId,
                    bookId = progress.bookId,
                    status = "SYNCED",
                    syncedAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                android.util.Log.e("VocabularyProgressSyncer", "Failed to sync book progress ${progress.bookId}", e)
            }
        }
    }
}

@Serializable
data class UserVocabularyWordLearningProgressDto(
    val user_id: String,
    val book_id: String,
    val word_id: String,
    val status: String,
    val last_studied_at: Long,
    val learned_at: Long?
)

@Serializable
data class UserVocabularyStudyRoundDto(
    val round_id: String,
    val user_id: String,
    val book_id: String,
    val status: String,
    val target_word_count: Int,
    val introduced_word_count: Int,
    val mastered_word_count: Int,
    val active_queue_size: Int,
    val next_word_sort_order_cursor: Int,
    val created_at: Long,
    val updated_at: Long
)

@Serializable
data class UserVocabularyBookProgressDto(
    val user_id: String,
    val book_id: String,
    val next_word_sort_order_cursor: Int,
    val active_round_id: String?,
    val learned_word_count: Int,
    val updated_at: Long
)
