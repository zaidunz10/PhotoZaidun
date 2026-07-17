package com.zaidun.photozaidun.data.processor.export

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.OutputStream

class ImageExporter(

    private val resolver: ContentResolver

) {

    fun export(

        folderUri: Uri,

        fileName: String,

        jpeg: ByteArray

    ): Uri {

        val fileUri = DocumentsContract.createDocument(

            resolver,

            folderUri,

            "image/jpeg",

            fileName

        ) ?: error("Gagal membuat file")

        resolver.openOutputStream(fileUri).use { stream ->

            stream ?: error("OutputStream null")

            stream.write(jpeg)

            stream.flush()

        }

        return fileUri

    }

}