package com.zaidun.photozaidun.data.local.dao

import androidx.room.*
import com.zaidun.photozaidun.data.local.entity.ProcessedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessedFileDao {

    @Query("SELECT * FROM processed_file WHERE historyId = :historyId")
    fun getFiles(historyId: Long): Flow<List<ProcessedFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: ProcessedFileEntity)
}