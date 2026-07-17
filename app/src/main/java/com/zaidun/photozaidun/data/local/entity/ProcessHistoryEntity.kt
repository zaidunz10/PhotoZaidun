package com.zaidun.photozaidun.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "process_history")
data class ProcessHistoryEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val folderName: String,

    val totalImages: Int,

    val successImages: Int,

    val failedImages: Int,

    val duration: Long,

    val uploadToDrive: Boolean,

    val createdAt: Long
)