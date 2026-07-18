package com.zaidun.photozaidun.data.drive

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

fun DocumentFile.copyToCache(
    context: Context
): File {

    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("Tidak dapat membuka file")

    val tempFile = File(
        context.cacheDir,
        name ?: "temp_${System.currentTimeMillis()}.jpg"
    )

    inputStream.use { input ->

        FileOutputStream(tempFile).use { output ->

            input.copyTo(output)

        }

    }

    return tempFile

}