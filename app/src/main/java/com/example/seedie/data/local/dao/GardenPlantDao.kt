package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.seedie.data.local.entity.GardenPlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenPlantDao {
    @Query(
        """
        SELECT * FROM garden_plants
        WHERE userId = :userId AND localDate = :localDate
        ORDER BY createdAt ASC
        """
    )
    fun observePlantsForDate(userId: String, localDate: String): Flow<List<GardenPlantEntity>>

    @Query(
        """
        SELECT * FROM garden_plants
        WHERE userId = :userId AND localDate >= :startDate AND localDate <= :endDate
        ORDER BY createdAt ASC
        """
    )
    fun observePlantsBetween(
        userId: String,
        startDate: String,
        endDate: String
    ): Flow<List<GardenPlantEntity>>

    @Query(
        """
        SELECT * FROM garden_plants
        WHERE userId = :userId AND sessionId = :sessionId
        LIMIT 1
        """
    )
    suspend fun findBySessionId(userId: String, sessionId: String): GardenPlantEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlant(plant: GardenPlantEntity): Long

    @Update
    suspend fun updatePlant(plant: GardenPlantEntity)
}
