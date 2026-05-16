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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(round: VocabularyStudyRoundEntity)
}
