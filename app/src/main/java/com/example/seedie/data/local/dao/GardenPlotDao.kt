package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.seedie.data.local.entity.GardenPlotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenPlotDao {
    @Query("SELECT * FROM garden_plots WHERE userId = :userId ORDER BY plotIndex ASC")
    fun getAllPlots(userId: String): Flow<List<GardenPlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlot(plot: GardenPlotEntity)

    @Update
    suspend fun updatePlot(plot: GardenPlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun initializePlots(plots: List<GardenPlotEntity>)
}