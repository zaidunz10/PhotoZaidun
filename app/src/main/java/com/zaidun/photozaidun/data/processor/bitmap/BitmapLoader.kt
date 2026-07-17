package com.zaidun.photozaidun.data.processor.bitmap

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

class BitmapLoader(

    private val resolver: ContentResolver

) {

    fun load(

        uri: Uri

    ): Bitmap {

        resolver.openInputStream(uri).use {

            return BitmapFactory.decodeStream(it)
                ?: error("Bitmap gagal dibaca")

        }

    }

}