package com.zaidun.photozaidun.worker

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.drive.Drive
import com.zaidun.photozaidun.data.auth.GoogleAuthManager
import com.zaidun.photozaidun.data.drive.DriveServiceFactory
import com.zaidun.photozaidun.data.drive.GoogleDriveRepository
import com.zaidun.photozaidun.data.drive.copyToCache
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.remote.SessionInsertDto
import com.zaidun.photozaidun.data.remote.SessionResponseDto
import com.zaidun.photozaidun.data.remote.SessionUpdateDriveDto
import com.zaidun.photozaidun.data.remote.SupabaseHelper
import com.zaidun.photozaidun.utils.SoundPlayer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.postgrest.postgrest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

private fun collectAllImageFiles(dir: DocumentFile): List<DocumentFile> {
    val result = mutableListOf<DocumentFile>()
    val files = dir.listFiles()
    for (f in files) {
        if (f.isDirectory) {
            result.addAll(collectAllImageFiles(f))
        } else if (f.isFile && (f.type?.startsWith("image/") == true || f.name?.matches(Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE)) == true)) {
            result.add(f)
        }
    }
    return result
}

@HiltWorker
class DriveUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesDataStore,
    private val driveServiceFactory: DriveServiceFactory,
    private val repository: GoogleDriveRepository,
    private val googleAuthManager: GoogleAuthManager
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val KEY_FOLDER_URI = "folder_uri"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_EXPECTED_COUNT = "expected_count"
        const val KEY_HOUR_FOLDER = "hour_folder"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "drive_upload_channel"
        private const val COMPLETED_CHANNEL_ID = "drive_upload_completed_channel"
        private const val UPLOAD_PARALLELISM = 25
    }

    override suspend fun doWork(): Result {
        Timber.tag("DriveUpload").d("=== DRIVE UPLOAD WORKER STARTED ===")
        SoundPlayer.playAssetSound(applicationContext, "memulai upload.mp3")

        val folderUriString = inputData.getString(KEY_FOLDER_URI)
        if (folderUriString.isNullOrBlank()) {
            Timber.e("Folder URI kosong")
            showFinalNotification(false, "Folder lokal tidak valid")
            return Result.failure()
        }

        val folderUri = Uri.parse(folderUriString)
        val folder = DocumentFile.fromTreeUri(applicationContext, folderUri)
            ?: return Result.failure()

        var token = preferences.driveAccessToken.first()
            .ifBlank { inputData.getString(KEY_ACCESS_TOKEN).orEmpty() }

        if (token.isBlank()) {
            val freshToken = googleAuthManager.getFreshAccessToken()
            if (!freshToken.isNullOrBlank()) {
                token = freshToken
                preferences.saveDriveAccessToken(freshToken)
            }
        }

        if (token.isBlank()) {
            Timber.e("Access Token kosong")
            showFinalNotification(false, "Token Google Drive tidak tersedia")
            return Result.failure()
        }

        val drive = driveServiceFactory.create(token)

        return try {
            // 1. ROOT FOLDER (Photo Zaidun)
            val rootName = preferences.rootFolder.first().ifBlank { "My Photo App" }
            val rootFolderObj = repository.getOrCreateFolderWithLink(drive, rootName)
            val rootId = rootFolderObj.id

            val folderLevels = preferences.folderLevels.first()
            var uploadParentId = rootId
            var lastCustomSubFolderLink: String? = null

            // 2. SUB FOLDER KUSTOM DARI SETTINGS (misal: "Touring" -> "KDMC")
            folderLevels
                .map { it.name.trim() }
                .filter { it.isNotBlank() }
                .forEach { folderName ->
                    val subFolderObj = repository.getOrCreateFolderWithLink(
                        drive,
                        folderName,
                        uploadParentId
                    )
                    uploadParentId = subFolderObj.id
                    lastCustomSubFolderLink = subFolderObj.webViewLink
                }

            // LINK SUPABASE: Gunakan Subfolder Terakhir di Settings ("KDMC") jika ada!
            // Jika tidak ada subfolder di Settings, gunakan Link Root Folder ("Photo Zaidun")!
            val finalDriveLink = lastCustomSubFolderLink.takeIf { !it.isNullOrBlank() }
                ?: rootFolderObj.webViewLink
                ?: ""

            val locationNameStr = inputData.getString("location")?.ifBlank { null } ?: "Sesi Fotografer"

            Timber.tag("DriveUpload").d("Location = $locationNameStr | Link Supabase = $finalDriveLink | Parent Drive ID = $uploadParentId")

            // UPDATE / SIMPAN KE SUPABASE DENGAN KUNCI UTAMA: LINK GOOGLE DRIVE (drive_folder_id)
            val isPublished = inputData.getBoolean("is_published", true)
            if (finalDriveLink.isNotBlank()) {
                try {
                    val userId = preferences.userId.first()
                    val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    val existingSessions = SupabaseHelper.client.postgrest["sessions"]
                        .select {
                            filter {
                                eq("photographer_id", userId)
                                eq("drive_folder_id", finalDriveLink)
                            }
                        }.decodeList<SessionResponseDto>()

                    val existingId = existingSessions.lastOrNull()?.id

                    if (existingId != null && existingId > 0L) {
                        val updateDto = SessionUpdateDriveDto(
                            driveFolderId = finalDriveLink,
                            isPublished = isPublished
                        )
                        SupabaseHelper.client.postgrest.from("sessions").update(updateDto) {
                            filter { eq("id", existingId) }
                        }
                        Timber.tag("DriveUpload").d("Supabase: Link Drive SAMA -> Reuse Sesi ID: $existingId")
                    } else {
                        val priceSingleVal = preferences.defaultPhotoPrice.first().takeIf { it > 0 } ?: 10000
                        val minQtyVal = preferences.minQtyDiscount.first()
                        val discountPriceVal = preferences.priceDiscount.first()
                        val wa = preferences.photographerWa.first()
                        val ig = preferences.photographerIg.first()

                        val newSessionDto = SessionInsertDto(
                            photographerId = userId,
                            location = locationNameStr,
                            sessionDate = todayDateStr,
                            driveFolderId = finalDriveLink,
                            whatsappNumber = wa,
                            instagramLink = ig,
                            priceSingle = priceSingleVal,
                            minQtyDiscount = minQtyVal,
                            priceDiscount = discountPriceVal,
                            isPublished = isPublished
                        )

                        val response = SupabaseHelper.client.postgrest["sessions"]
                            .insert(newSessionDto) {
                                select()
                            }.decodeSingle<SessionResponseDto>()

                        Timber.tag("DriveUpload").d("Supabase: Link Drive BERBEDA -> Berhasil buat Sesi BARU ID: ${response.id}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Supabase: Gagal menyimpan/mengecek sesi berdasarkan Link Drive")
                }
            }

            // -----------------------------------------------------
            // 3. BUAT FOLDER JAM EXIF & PART FOLDER SECARA PRESISI
            // -----------------------------------------------------
            val folderName = folder.name ?: "06.00 - 07.00"
            val isPart = folderName.startsWith("part", ignoreCase = true)

            val targetDriveFolderId = if (isPart) {
                // Jika Part AKTIF (folder = "part 1"):
                // 1. Ambil nama folder jam dari inputData.getString(KEY_HOUR_FOLDER) terlebih dahulu
                val inputHour = inputData.getString(KEY_HOUR_FOLDER)?.ifBlank { null }
                val uriSegmentHour = folderUri.lastPathSegment?.split("/")?.let { segs ->
                    val idx = segs.indexOfLast { it.startsWith("part", ignoreCase = true) }
                    if (idx > 0) segs[idx - 1] else null
                }?.ifBlank { null }

                val hourFolderName = inputHour
                    ?: uriSegmentHour
                    ?: folder.parentFile?.name?.ifBlank { null }
                    ?: "06.00 - 07.00"

                val hourFolderId = repository.getOrCreateFolder(drive, hourFolderName, uploadParentId)

                // 2. Buat Part Folder ("part 1") di dalam Folder Jam EXIF
                repository.getOrCreateFolder(drive, folderName, hourFolderId)
            } else {
                // Jika Part MATI (folder = "06.00 - 07.00"):
                val hourFolderName = inputData.getString(KEY_HOUR_FOLDER)?.ifBlank { null }
                    ?: folderName
                repository.getOrCreateFolder(drive, hourFolderName, uploadParentId)
            }

            Timber.tag("DriveUpload").d("Target Upload Folder ID = $targetDriveFolderId ($folderName)")

            val filesToUpload = collectAllImageFiles(folder)
            val totalFiles = filesToUpload.size

            if (totalFiles == 0) {
                showFinalNotification(true, "Tidak ada foto untuk diupload")
                return Result.success()
            }

            Timber.tag("DriveUpload").d("Total foto lokal yang akan di-upload = $totalFiles di folder $folderName")

            val totalUploaded = AtomicInteger(0)
            uploadDirectoryRecursively(
                drive = drive,
                localDir = folder,
                parentDriveFolderId = targetDriveFolderId,
                totalUploaded = totalUploaded,
                totalFilesCount = totalFiles
            )

            showFinalNotification(true, "Berhasil mengunggah $totalFiles foto ke Google Drive")
            Result.success()

        } catch (e: GoogleJsonResponseException) {
            Timber.tag("DriveUpload").e(e, "Google Drive API Error (code=${e.statusCode})")
            if (e.statusCode == 401) {
                val freshToken = googleAuthManager.getFreshAccessToken()
                if (!freshToken.isNullOrBlank()) {
                    preferences.saveDriveAccessToken(freshToken)
                    return Result.retry()
                }
            }
            showFinalNotification(false, "Gagal upload: ${e.details?.message ?: e.message}")
            Result.retry()
        } catch (e: Exception) {
            Timber.tag("DriveUpload").e(e, "Unexpected Upload Error")
            showFinalNotification(false, "Gagal upload: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun uploadDirectoryRecursively(
        drive: Drive,
        localDir: DocumentFile,
        parentDriveFolderId: String,
        totalUploaded: AtomicInteger,
        totalFilesCount: Int,
        isRoot: Boolean = false
    ) {
        val items = localDir.listFiles()

        // 1. Upload foto lokal pada folder ini
        val localImages = items.filter { it.isFile && (it.type?.startsWith("image/") == true || it.name?.matches(Regex(".*\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE)) == true) }
            .sortedBy { it.name?.lowercase() ?: "" }

        if (localImages.isNotEmpty()) {
            val existingFiles = repository.getAllFileNames(drive, parentDriveFolderId)
            val semaphore = Semaphore(UPLOAD_PARALLELISM)

            supervisorScope {
                localImages.map { document ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            if (isStopped) return@withPermit
                            val fileName = document.name ?: "image.jpg"
                            if (fileName !in existingFiles) {
                                var tempCacheFile: File? = null
                                try {
                                    tempCacheFile = document.copyToCache(applicationContext)
                                    if (tempCacheFile != null) {
                                        var uploadSuccess = false
                                        var attempts = 0
                                        while (!uploadSuccess && attempts < 3 && !isStopped) {
                                            try {
                                                repository.uploadFile(
                                                    drive = drive,
                                                    localFile = tempCacheFile,
                                                    parentFolderId = parentDriveFolderId
                                                )
                                                uploadSuccess = true
                                            } catch (e: Exception) {
                                                attempts++
                                                if (attempts >= 3) break
                                                delay(1000)
                                            }
                                        }

                                        if (uploadSuccess) {
                                            val count = totalUploaded.incrementAndGet()
                                            updateNotificationProgress(count, totalFilesCount, localDir.name ?: "")
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Gagal upload $fileName")
                                } finally {
                                    tempCacheFile?.delete()
                                }
                            }
                        }
                    }
                }.awaitAll()
            }
        }

        // 2. Buat sub-folder Google Drive secara rekursif untuk Jam EXIF & Part
        val subDirs = items.filter { it.isDirectory }
        subDirs.forEach { subDir ->
            if (isStopped) return
            val dirName = subDir.name ?: "subfolder"
            val driveSubFolderId = repository.getOrCreateFolder(drive, dirName, parentDriveFolderId)
            uploadDirectoryRecursively(
                drive = drive,
                localDir = subDir,
                parentDriveFolderId = driveSubFolderId,
                totalUploaded = totalUploaded,
                totalFilesCount = totalFilesCount,
                isRoot = false
            )
        }
    }

    private suspend fun updateNotificationProgress(current: Int, total: Int, folderName: String) {
        val progressText = "Mengunggah ($folderName) $current/$total foto"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setForeground(
                    ForegroundInfo(
                        NOTIFICATION_ID,
                        createForegroundInfo(progressText).notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                )
            } else {
                setForeground(createForegroundInfo(progressText))
            }
        } catch (e: Exception) {
            Timber.e(e, "Gagal update notification")
        }
    }

    private fun createForegroundInfo(progressText: String): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Watermark Pro upload ke google drive")
            .setContentText(progressText)
            .setSmallIcon(R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showFinalNotification(isSuccess: Boolean, message: String) {
        createCompletedNotificationChannel()

        if (isSuccess) {
            SoundPlayer.playAssetSound(applicationContext, "upload selesai.mp3")
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val title = if (isSuccess) "Upload Selesai 🎉" else "Upload Gagal ⚠️"
        val icon = if (isSuccess) R.drawable.stat_sys_upload_done else R.drawable.stat_notify_error

        val notification = NotificationCompat.Builder(applicationContext, COMPLETED_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setSound(soundUri)
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Google Drive Upload"
            val descriptionText = "Notifikasi status unggah foto ke Google Drive"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createCompletedNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Upload Drive Selesai"
            val descriptionText = "Notifikasi suara saat upload Google Drive selesai"
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(COMPLETED_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = descriptionText
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
