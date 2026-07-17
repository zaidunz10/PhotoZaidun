package com.zaidun.photozaidun.data.local.dao

import androidx.room.*
import com.zaidun.photozaidun.data.local.entity.WatermarkTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatermarkTemplateDao {

    @Query("SELECT * FROM watermark_template")
    fun getAll(): Flow<List<WatermarkTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: WatermarkTemplateEntity)
}