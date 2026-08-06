package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.GardenUnlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenUnlockDao {
    @Query("SELECT * FROM garden_unlocks WHERE userId = :userId")
    fun observeUnlocks(userId: String): Flow<List<GardenUnlockEntity>>

    @Query(
        """
        SELECT * FROM garden_unlocks
        WHERE userId = :userId AND speciesId = :speciesId
        LIMIT 1
        """
    )
    suspend fun findUnlock(userId: String, speciesId: String): GardenUnlockEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(unlock: GardenUnlockEntity): Long
}
