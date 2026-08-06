package com.zaidun.photozaidun.data.scanner

import android.net.Uri

data class ScanPhoto(
    val uri: Uri,
    val name: String,
    val mime: String,
    val size: Long
)