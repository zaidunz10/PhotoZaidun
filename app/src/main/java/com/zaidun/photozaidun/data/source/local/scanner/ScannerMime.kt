package com.zaidun.photozaidun.data.source.local.scanner

object ScannerMime {

    private val supported = setOf(

        "image/jpeg",

        "image/png",

        "image/webp"

    )

    fun isImage(
        mimeType: String
    ): Boolean {

        return mimeType in supported
    }

}