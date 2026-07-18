package com.zaidun.photozaidun.data.drive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import timber.log.Timber
import java.net.URLConnection
import java.io.File as JavaFile
import javax.inject.Inject

class GoogleDriveRepository @Inject constructor() {

    fun findFolder(drive: Drive, folderName: String, parentId: String? = null): String? {
        val query = buildString {
            append("mimeType='application/vnd.google-apps.folder'")
            append(" and trashed=false")
            append(" and name='$folderName'")

            if (parentId != null) {
                append(" and '$parentId' in parents")
            } else {
                // BARIS INI PENTING: Cari hanya di 'My Drive' utama
                append(" and 'root' in parents")
            }
        }

        val result = drive.files().list()
            .setQ(query)
            .setFields("files(id,name)")
            .execute()

        return result.files.firstOrNull()?.id
    }

    fun createFolder(
        drive: Drive,
        folderName: String,
        parentId: String? = null
    ): String {

        val metadata = File().apply {
            name = folderName
            mimeType = "application/vnd.google-apps.folder"

            if (parentId != null) {
                parents = listOf(parentId)
            }
        }

        return drive.files()
            .create(metadata)
            .setFields("id")
            .execute()
            .id
    }

    fun getOrCreateFolder(
        drive: Drive,
        folderName: String,
        parentId: String? = null
    ): String {

        return findFolder(
            drive,
            folderName,
            parentId
        ) ?: createFolder(
            drive,
            folderName,
            parentId
        )
    }

    fun uploadFile(
        drive: Drive,
        localFile: JavaFile,
        parentFolderId: String? = null
    ): String {
        val metadata = File().apply {
            name = localFile.name
            if (parentFolderId != null) {
                parents = listOf(parentFolderId)
            }
        }

        val mimeType = URLConnection.guessContentTypeFromName(localFile.name)
            ?: "application/octet-stream"

        val media = FileContent(mimeType, localFile)

        // UBAH DARI SINI KE BAWAH:
        val request = drive.files().create(metadata, media)
            .setFields("id,name")

        // Baris kunci untuk mencegah error "File Not Found":
        request.mediaHttpUploader.isDirectUploadEnabled = true

        val uploaded = request.execute()
        // SAMPAI SINI

        Timber.d("Upload berhasil: ${uploaded.name} (${uploaded.id})")
        return uploaded.id
    }
}