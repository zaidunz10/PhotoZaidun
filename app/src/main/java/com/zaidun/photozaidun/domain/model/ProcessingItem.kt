package com.zaidun.photozaidun.domain.model

import android.graphics.Bitmap
import android.net.Uri

data class ProcessingItem(

    val photo: PhotoItem,

    var bitmap: Bitmap? = null,
    var outputUri: Uri? = null,
    var jpegBytes: ByteArray? = null

)