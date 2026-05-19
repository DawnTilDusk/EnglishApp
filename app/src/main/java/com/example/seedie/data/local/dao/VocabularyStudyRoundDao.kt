package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.VocabularyStudyRoundEntity

@Dao
interface VocabularyStudyRoundDao {
    @Query("SELECT * FROM vocabulary_study_rounds WHERE roundId = :roundId LIMIT 1")
    suspend fun getRoundById(roundId: String): VocabularyStudyRoundEntity?

    @Query(
        "SELECT * FROM vocabulary_study_rounds " +
            "WHERE userId = :userId AND bookId = :bookId AND status = :status " +
            "ORDER BY updatedAt DESC LIMIT 1"
    )
    suspend fun getLatestRoundByStatus(
        userId: String,
        bookId: String,
        status: String
    ): VocabularyStudyRoundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(round: VocabularyStudyRoundEntity)

    @Query("SELECT * FROM vocabulary_study_rounds WHERE userId = :userId AND syncStatus = 'PENDING'")
    suspend fun getPendingRounds(userId: String): List<VocabularyStudyRoundEntity>

    @Query("UPDATE vocabulary_study_rounds SET syncStatus = :status, syncedAt = :syncedAt WHERE roundId = :roundId")
    suspend fun updateSyncStatus(roundId: String, status: String, syncedAt: Long?)
}
