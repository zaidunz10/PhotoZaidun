package com.zaidun.photozaidun.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watermark_template")
data class WatermarkTemplateEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val templateName: String,

    val text: String,

    val opacity: Float,

    val textSize: Float,

    val position: String
)