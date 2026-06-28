package com.example.seedie.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.seedie.data.local.entity.SyncOperationEntity

@Dao
interface SyncOperationDao {
    @Query("SELECT * FROM sync_operations WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<SyncOperationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncOperationEntity)

    @Query("DELETE FROM sync_operations WHERE operationId = :id")
    suspend fun delete(id: String)

    @Query("UPDATE sync_operations SET status = 'FAILED', retryCount = retryCount + 1 WHERE operationId = :id")
    suspend fun markFailed(id: String)
}
