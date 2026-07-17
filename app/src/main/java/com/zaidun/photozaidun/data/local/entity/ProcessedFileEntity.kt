package com.zaidun.photozaidun.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processed_file")
data class ProcessedFileEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val historyId: Long,

    val originalPath: String,

    val outputPath: String,

    val fileSizeBefore: Long,

    val fileSizeAfter: Long,

    val success: Boolean
)