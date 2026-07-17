package com.zaidun.photozaidun.data.source.local.scanner

import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import com.zaidun.photozaidun.domain.model.PhotoItem

object CursorPhotoMapper {

    fun fromCursor(
        cursor: Cursor,
        treeUri: Uri
    ): PhotoItem {

        val documentId =
            cursor.getString(
                cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
                )
            )

        return PhotoItem(

            uri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                documentId
            ),

            name = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )
            ),

            size = cursor.getLong(
                cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_SIZE
                )
            ),

            mimeType = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )
            ),

            lastModified = cursor.getLong(
                cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED
                )
            )
        )
    }
}