package com.zaidun.photozaidun.data.processor.compress

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

class JpegCompressor {

    fun compress(

        bitmap: Bitmap,

        quality: Int

    ): ByteArray {

        val output = ByteArrayOutputStream()

        bitmap.compress(

            Bitmap.CompressFormat.JPEG,

            quality,

            output

        )

        return output.toByteArray()

    }

}