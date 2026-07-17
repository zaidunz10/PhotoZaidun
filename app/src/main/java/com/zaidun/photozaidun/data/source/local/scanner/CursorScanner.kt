package com.zaidun.photozaidun.data.source.local.scanner

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.zaidun.photozaidun.domain.model.PhotoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CursorScanner(

    private val resolver: ContentResolver

) {

    fun scan(
        treeUri: Uri
    ): Flow<PhotoItem> = flow {

        val queue = FolderQueue()

        queue.enqueue(
            SafQuery.getRootDocumentId(treeUri)
        )

        while (!queue.isEmpty()) {

            val currentFolderId =
                queue.dequeue()

            val childrenUri =
                SafQuery.buildChildrenUri(
                    treeUri,
                    currentFolderId
                )

            resolver.query(

                childrenUri,

                SafColumns.PROJECTION,

                null,

                null,

                null

            )?.use { cursor ->

                val documentIdIndex =
                    cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID
                    )

                val nameIndex =
                    cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    )

                val sizeIndex =
                    cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_SIZE
                    )

                val mimeIndex =
                    cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    )

                val modifiedIndex =
                    cursor.getColumnIndexOrThrow(
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED
                    )

                while (cursor.moveToNext()) {

                    val documentId =
                        cursor.getString(documentIdIndex)

                    val mime =
                        cursor.getString(mimeIndex)

                    when {

                        mime ==
                                DocumentsContract.Document.MIME_TYPE_DIR -> {

                            queue.enqueue(
                                documentId
                            )

                        }

                        ScannerMime.isImage(
                            mime
                        ) -> {

                            emit(

                                PhotoItem(

                                    uri =
                                        SafQuery.buildDocumentUri(
                                            treeUri,
                                            documentId
                                        ),

                                    name =
                                        cursor.getString(
                                            nameIndex
                                        ),

                                    size =
                                        cursor.getLong(
                                            sizeIndex
                                        ),

                                    mimeType =
                                        mime,

                                    lastModified =
                                        cursor.getLong(
                                            modifiedIndex
                                        )

                                )

                            )

                        }

                    }

                }

            }

        }

    }

}