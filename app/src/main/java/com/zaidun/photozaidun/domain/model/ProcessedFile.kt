package com.zaidun.photozaidun.domain.model

data class ProcessedFile(

    val id: Long = 0,

    val historyId: Long,

    val originalPath: String,

    val outputPath: String,

    val fileSizeBefore: Long,

    val fileSizeAfter: Long,

    val success: Boolean
)