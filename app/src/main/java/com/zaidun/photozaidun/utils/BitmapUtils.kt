package com.zaidun.photozaidun.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.exifinterface.media.ExifInterface

object BitmapUtils {

    fun loadBitmap(
        context: Context,
        uri: Uri
    ): Bitmap {

        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Bitmap gagal dibaca")

        val bitmap = BitmapFactory.decodeStream(input)
        input.close()

        val rotation = getRotation(context, uri)

        if (rotation == 0) return bitmap

        val matrix = Matrix().apply {
            postRotate(rotation.toFloat())
        }

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun getRotation(
        context: Context,
        uri: Uri
    ): Int {

        val input =
            context.contentResolver.openInputStream(uri)
                ?: return 0

        val exif = ExifInterface(input)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        Log.d(
            "EXIF",
            "orientation=$orientation"
        )
        input.close()

        return when (
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        ) {

            ExifInterface.ORIENTATION_ROTATE_90 -> 90

            ExifInterface.ORIENTATION_ROTATE_180 -> 180

            ExifInterface.ORIENTATION_ROTATE_270 -> 270


            else -> 0

        }

    }
}