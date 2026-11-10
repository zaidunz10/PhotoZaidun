
package com.zaidun.carifoto.data.remote

object CariFotoDriveHelper {

    fun getFolderUrl(folderId: String?): String? {
        if (folderId.isNullOrBlank()) return null

        return "https://drive.google.com/drive/folders/$folderId"
    }

    fun getThumbnailUrl(fileId: String?): String? {
        if (fileId.isNullOrBlank()) return null

        return "https://drive.google.com/thumbnail?id=$fileId&sz=w800"
    }

    fun getFileUrl(fileId: String?): String? {
        if (fileId.isNullOrBlank()) return null

        return "https://drive.google.com/uc?id=$fileId"
    }
}
