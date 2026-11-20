package com.zaidun.photozaidun.data.scanner

import android.content.Context
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FastScanner {
    private const val SORT_FILES = false
    data class ScanResult(
        val images: List<ScanPhoto>
    )

    suspend fun scan(
        context: Context,
        folder: DocumentFile
    ): ScanResult = withContext(Dispatchers.IO) {

        val resolver = context.contentResolver

        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                folder.uri,
                DocumentsContract.getTreeDocumentId(folder.uri)
            )

        val images = ArrayList<ScanPhoto>(5000)
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
            ),
            null,
            null,
            null
        )?.use { cursor ->

            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val lastModifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeIndex) ?: ""

                if (!mime.startsWith("image/"))
                    continue

                val docId = cursor.getString(idIndex)
                val uri = DocumentsContract.buildDocumentUriUsingTree(folder.uri, docId)
                val lastMod = if (lastModifiedIndex >= 0) cursor.getLong(lastModifiedIndex) else 0L

                images.add(
                    ScanPhoto(
                        uri = uri,
                        name = cursor.getString(nameIndex) ?: "",
                        mime = mime,
                        size = cursor.getLong(sizeIndex),
                        lastModified = lastMod
                    )
                )
            }
        }

        if (SORT_FILES) {
            images.sortBy { it.name }
        }

        ScanResult(images)
    }
}
