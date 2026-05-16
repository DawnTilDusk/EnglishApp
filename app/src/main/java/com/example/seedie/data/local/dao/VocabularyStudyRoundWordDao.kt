package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.VocabularyStudyRoundWordEntity

@Dao
interface VocabularyStudyRoundWordDao {
    @Query("SELECT * FROM vocabulary_study_round_words WHERE roundId = :roundId ORDER BY queueOrder ASC")
    suspend fun getRoundWords(roundId: String): List<VocabularyStudyRoundWordEntity>

    @Query(
        "SELECT * FROM vocabulary_study_round_words " +
            "WHERE roundId = :roundId AND isMasteredInRound = 0 " +
            "ORDER BY queueOrder ASC"
    )
    suspend fun getActiveRoundWords(roundId: String): List<VocabularyStudyRoundWordEntity>

    @Query(
        "SELECT * FROM vocabulary_study_round_words " +
            "WHERE roundId = :roundId AND isMasteredInRound = 1 " +
            "ORDER BY updatedAt ASC, queueOrder ASC"
    )
    suspend fun getMasteredRoundWords(roundId: String): List<VocabularyStudyRoundWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(roundWords: List<VocabularyStudyRoundWordEntity>)

    @Query("DELETE FROM vocabulary_study_round_words WHERE roundId = :roundId")
    suspend fun deleteByRoundId(roundId: String)
}
