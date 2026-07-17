package com.zaidun.photozaidun.data.processor.resize

import android.graphics.Bitmap

class ImageResizer {

    fun resize(

        bitmap: Bitmap,

        percent: Int

    ): Bitmap {

        val width =
            bitmap.width * percent / 100

        val height =
            bitmap.height * percent / 100

        return Bitmap.createScaledBitmap(

            bitmap,

            width,

            height,

            true

        )

    }

}