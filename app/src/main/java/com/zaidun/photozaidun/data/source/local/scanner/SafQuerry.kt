package com.zaidun.photozaidun.data.source.local.scanner

import android.net.Uri
import android.provider.DocumentsContract

object SafQuery {

    fun getRootDocumentId(
        treeUri: Uri
    ): String {

        return DocumentsContract.getTreeDocumentId(treeUri)
    }

    fun buildChildrenUri(
        treeUri: Uri,
        documentId: String
    ): Uri {

        return DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            documentId
        )
    }

    fun buildDocumentUri(
        treeUri: Uri,
        documentId: String
    ): Uri {

        return DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            documentId
        )
    }
}