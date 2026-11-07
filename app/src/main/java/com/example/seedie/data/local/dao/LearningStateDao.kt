package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.DailyWordCompletionEntity
import com.example.seedie.data.local.entity.PracticeDraftEntity
import com.example.seedie.data.local.entity.StudySettlementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningStateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordWord(row: DailyWordCompletionEntity): Long

    @Query("SELECT COUNT(*) FROM daily_word_completions WHERE userId = :userId AND date = :date")
    suspend fun wordCount(userId: String, date: String): Int

    @Query("SELECT COUNT(*) FROM daily_word_completions WHERE userId = :userId AND date = :date")
    fun observeWordCount(userId: String, date: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(row: StudySettlementEntity): Long

    @Query("SELECT * FROM study_settlements WHERE userId = :userId AND settled = 0 ORDER BY createdAt")
    suspend fun pending(userId: String): List<StudySettlementEntity>

    @Query("UPDATE study_settlements SET settled = 1 WHERE userId = :userId AND settlementId = :id")
    suspend fun markSettled(userId: String, id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(row: PracticeDraftEntity)

    @Query("SELECT * FROM practice_drafts WHERE userId = :userId AND draftKey = :key")
    suspend fun draft(userId: String, key: String): PracticeDraftEntity?

    @Query("DELETE FROM practice_drafts WHERE userId = :userId AND draftKey = :key")
    suspend fun deleteDraft(userId: String, key: String)

    @Query("DELETE FROM practice_drafts WHERE userId = :userId AND draftKey LIKE 'vocab:%'")
    suspend fun clearVocabularyDrafts(userId: String)

    @Query("DELETE FROM practice_drafts WHERE (draftKey LIKE 'event:%' OR draftKey LIKE 'feedback:%') AND updatedAt < :before")
    suspend fun pruneDiagnostics(before: Long)
}
