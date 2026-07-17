package com.zaidun.photozaidun.data.source.local.scanner

import android.provider.DocumentsContract

object SafColumns {

    val PROJECTION = arrayOf(

        DocumentsContract.Document.COLUMN_DOCUMENT_ID,

        DocumentsContract.Document.COLUMN_DISPLAY_NAME,

        DocumentsContract.Document.COLUMN_SIZE,

        DocumentsContract.Document.COLUMN_MIME_TYPE,

        DocumentsContract.Document.COLUMN_LAST_MODIFIED

    )

}