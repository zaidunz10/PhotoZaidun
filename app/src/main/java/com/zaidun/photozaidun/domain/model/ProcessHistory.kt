package com.zaidun.photozaidun.domain.model

data class ProcessHistory(

    val id: Long = 0,

    val folderName: String,

    val totalImages: Int,

    val successImages: Int,

    val failedImages: Int,

    val duration: Long,

    val uploadToDrive: Boolean,

    val createdAt: Long,
    val date: Long,
    val totalPhoto: Int,

    val success: Int,

    val failed: Int
)