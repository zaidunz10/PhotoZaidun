package com.zaidun.photozaidun.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.ForegroundInfo
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.zaidun.photozaidun.data.drive.DriveServiceFactory
import com.zaidun.photozaidun.data.drive.GoogleDriveRepository
import com.zaidun.photozaidun.data.drive.copyToCache
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class DriveUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesDataStore,
    private val driveServiceFactory: DriveServiceFactory,
    private val repository: GoogleDriveRepository
) : CoroutineWorker(context, params) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_FOLDER_URI = "folder_uri"
        const val KEY_ACCESS_TOKEN = "access_token"
        private const val CHANNEL_ID = "drive_upload_channel"
        private const val NOTIFICATION_ID = 101
    }


    override suspend fun doWork(): Result {
        createNotificationChannel()
        try {
            setForeground(createForegroundInfo("Menyiapkan upload..."))
        } catch (e: Exception) {
            Timber.e("Gagal setForeground: ${e.message}")
        }

        Timber.tag("DriveDr").d("=== DRIVE WORKER STARTED ===")

        // Ambil URI folder yang dikirim dari BatchProcessWorker
        val folderUriStr = inputData.getString("folder_uri") ?: return Result.failure()
        val folderUri = Uri.parse(folderUriStr)

        val token =
            inputData.getString(KEY_ACCESS_TOKEN)
                ?: return Result.failure()
        Timber.d("Drive Token = ${token.take(20)}...")
        if (token.isBlank()) {

            Timber.e("Access Token kosong")

            showFinalNotification(false, "Token akses tidak tersedia")
            return Result.failure()
        }
        val drive = driveServiceFactory.create(token)




        // Memerlukan import androidx.documentfile.provider.DocumentFile
        val folder = DocumentFile.fromTreeUri(applicationContext, folderUri) ?: return Result.failure()

        return try {
            // AMBIL INPUT USER DARI DATASTORE
            val rootName = preferences.rootFolder.first().ifBlank { "My Photo App" }
            val mainName = preferences.mainFolder.first().ifBlank { "Uncategorized" }

            // LEVEL 1: Folder paling atas (sesuai input user)
            val rootId = repository.getOrCreateFolder(drive, rootName)

            // LEVEL 2: Folder di dalamnya (misal: "Sunmori" atau "Kamar")
            val mainId = repository.getOrCreateFolder(drive, mainName, rootId)

            // LEVEL 3: Folder Part (part_1, part_2, dst)
            // folder.name adalah nama folder lokal yang sudah mengandung prefix (misal: "bagian_1")
            val partFolderId = repository.getOrCreateFolder(drive, folder.name ?: "part_1", mainId)

            // ... sisa kode upload file ke partFolderId ...

            Timber.d("Part Folder : $partFolderId")

            Timber.d("Memulai upload folder ke Google Drive: ${folder.name}")

            val filesToUpload = folder.listFiles().filter { it.type?.startsWith("image/") == true }
            val totalFiles = filesToUpload.size
            filesToUpload.forEachIndexed { index, document ->
                val progressText = "Mengunggah ${index + 1}/$totalFiles foto: ${document.name}"
                setForeground(createForegroundInfo(progressText))

                val tempFile = document.copyToCache(applicationContext)
                try {
                    repository.uploadFile(drive, tempFile, partFolderId)
                } finally {
                    tempFile.delete()
                }
            }
            showFinalNotification(
                true,
                "$totalFiles foto selesai di upload"
            )

            Result.success()
        } catch (e: GoogleJsonResponseException) {

            return when (e.statusCode) {

                401 -> {

                    Timber.e("Access Token expired")

                    Result.retry()

                }

                403 -> {

                    Timber.e("Permission denied")

                    showFinalNotification(
                        false,
                        "Google Drive permission denied"
                    )

                    Result.failure()

                }

                404 -> {

                    Timber.e("Folder tidak ditemukan")

                    showFinalNotification(
                        false,
                        "Folder Drive tidak ditemukan"
                    )

                    Result.failure()

                }

                else -> {

                    Timber.e(e)

                    Result.retry()

                }

            }

        }
    }
    private fun createForegroundInfo(progress: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Photo Zaidun - Uploading")
            .setTicker("Mengunggah ke Drive")
            .setContentText(progress)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Google Drive Upload",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Status unggah foto ke Google Drive"
            enableLights(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun showFinalNotification(success: Boolean, message: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(if (success) "Upload Selesai" else "Upload Gagal")
            .setContentText(message)
            .setSmallIcon(if (success) android.R.drawable.stat_sys_upload_done else android.R.drawable.stat_notify_error)
            .setAutoCancel(false)
            .setOngoing(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}