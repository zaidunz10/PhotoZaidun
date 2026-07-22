package com.zaidun.photozaidun.data.drive
import com.zaidun.photozaidun.domain.model.DriveStorageInfo

import android.R.attr.query
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import timber.log.Timber
import java.net.URLConnection
import java.io.File as JavaFile
import javax.inject.Inject

class GoogleDriveRepository @Inject constructor() {
    suspend fun getStorageInfo(drive: Drive

    ): DriveStorageInfo {

        val about = drive.about()
            .get()
            .setFields("user,storageQuota")
            .execute()

        val quota = about.storageQuota

        return DriveStorageInfo(

            email = about.user.emailAddress,

            totalBytes = quota.limit,

            usedBytes = quota.usage

        )

    }

    fun findFolder(
        drive: Drive,
        folderName: String,
        parentId: String? = null
    ): String? {

        val cleanName = folderName.trim()

        val query = buildString {
            Timber.d("Query: $query")
            append("mimeType='application/vnd.google-apps.folder'")
            append(" and trashed=false")
            append(" and name='$cleanName'")

            if (parentId != null) {
                append(" and '$parentId' in parents")
            } else {
                append(" and 'root' in parents")
            }
        }

        val result = drive.files()
            .list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id,name,createdTime)")
            .execute()

        val folders = result.files ?: emptyList()

        if (folders.isEmpty()) {
            Timber.d("Folder '$cleanName' tidak ditemukan")
            return null
        }

        // Jika ada lebih dari satu folder dengan nama sama,
        // gunakan yang paling lama dibuat.
        val selected = folders.minByOrNull { it.createdTime.value }!!

        Timber.d("Menggunakan folder ${selected.name} (${selected.id})")

        return selected.id
    }
    fun findFolders(
        drive: Drive,
        folderName: String,
        parentId: String? = null

    ): List<File> {


        val cleanName = folderName.trim()

        val query = buildString {

            append("mimeType='application/vnd.google-apps.folder'")
            append(" and trashed=false")
            append(" and name='$cleanName'")

            if (parentId != null) {
                append(" and '$parentId' in parents")
            } else {
                append(" and 'root' in parents")
            }
        }

        return drive.files()
            .list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id,name,createdTime)")
            .execute()
            .files ?: emptyList()
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

        val folderId = findFolder(
            drive,
            folderName,
            parentId
        )

        return if (folderId != null) {

            Timber.d("Folder ditemukan")

            folderId

        } else {

            Timber.d("Folder belum ada, membuat folder baru")

            createFolder(
                drive,
                folderName,
                parentId
            )
        }
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