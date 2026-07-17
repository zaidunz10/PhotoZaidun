package com.zaidun.photozaidun.data.local.dao

import androidx.room.*
import com.zaidun.photozaidun.data.local.entity.ProcessHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessHistoryDao {

    @Query("SELECT * FROM process_history ORDER BY createdAt DESC")
    fun getAll(): Flow<List<ProcessHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: ProcessHistoryEntity)
}