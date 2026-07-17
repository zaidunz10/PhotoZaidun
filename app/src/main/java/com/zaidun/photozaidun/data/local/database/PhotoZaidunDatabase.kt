package com.zaidun.photozaidun.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zaidun.photozaidun.data.local.dao.PresetDao
import com.zaidun.photozaidun.data.local.dao.ProcessHistoryDao
import com.zaidun.photozaidun.data.local.dao.ProcessedFileDao
import com.zaidun.photozaidun.data.local.dao.WatermarkTemplateDao
import com.zaidun.photozaidun.data.local.entity.PresetEntity
import com.zaidun.photozaidun.data.local.entity.ProcessHistoryEntity
import com.zaidun.photozaidun.data.local.entity.ProcessedFileEntity
import com.zaidun.photozaidun.data.local.entity.WatermarkTemplateEntity

@Database(
    entities = [
        PresetEntity::class,
        ProcessHistoryEntity::class,
        ProcessedFileEntity::class,
        WatermarkTemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PhotoZaidunDatabase : RoomDatabase() {

    abstract fun presetDao(): PresetDao

    abstract fun processHistoryDao(): ProcessHistoryDao

    abstract fun processedFileDao(): ProcessedFileDao

    abstract fun watermarkTemplateDao(): WatermarkTemplateDao
}