package com.zaidun.photozaidun.domain.model

import android.net.Uri

data class PhotoItem(

    val uri: Uri,

    val name: String,

    val size: Long,

    val mimeType: String,

    val lastModified: Long,

    val width: Int? = null,

    val height: Int? = null

)