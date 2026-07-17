package com.zaidun.photozaidun.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preset")
data class PresetEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val resizePercent: Int,

    val compressionQuality: Int,

    val watermarkEnabled: Boolean,

    val watermarkText: String,

    val uploadToDrive: Boolean,

    val createdAt: Long
)