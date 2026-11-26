package com.zaidun.photozaidun.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_ai_metadata")
data class PhotoAiMetadataEntity(
    @PrimaryKey val fileName: String,
    val bikeModel: String? = null,
    val plateNumber: String? = null,
    val bikeColor: String? = null,
    val jacketColor: String? = null,
    val exifTime: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
