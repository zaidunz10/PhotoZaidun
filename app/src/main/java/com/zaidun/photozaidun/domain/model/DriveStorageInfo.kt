package com.zaidun.photozaidun.domain.model

data class DriveStorageInfo(
    val email: String,

    val totalBytes: Long,

    val usedBytes: Long

) {

    val freeBytes: Long
        get() = totalBytes - usedBytes

    val percent: Float
        get() =
            if (totalBytes == 0L)
                0f
            else
                usedBytes.toFloat() / totalBytes.toFloat()

}