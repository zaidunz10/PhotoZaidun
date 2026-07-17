package com.zaidun.photozaidun.data.local.dao

import androidx.room.*
import com.zaidun.photozaidun.data.local.entity.PresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {

    @Query("SELECT * FROM preset ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: PresetEntity)

    @Delete
    suspend fun delete(preset: PresetEntity)
}