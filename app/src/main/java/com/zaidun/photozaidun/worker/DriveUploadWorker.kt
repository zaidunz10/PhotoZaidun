package com.zaidun.photozaidun.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.zaidun.photozaidun.data.drive.DriveServiceFactory
import com.zaidun.photozaidun.data.drive.GoogleDriveRepository
import com.zaidun.photozaidun.data.drive.copyToCache
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@HiltWorker
class DriveUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesDataStore,
    private val driveServiceFactory: DriveServiceFactory,
    private val repository: GoogleDriveRepository
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

    companion object {

        const val KEY_FOLDER_URI = "folder_uri"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_EXPECTED_COUNT = "expected_count"

        private const val CHANNEL_ID = "drive_upload_channel"
        private const val NOTIFICATION_ID = 101

        // JUMLAH UPLOAD BERSAMAAN
        private const val UPLOAD_PARALLELISM = 20
    }

    override suspend fun doWork(): Result {

        createNotificationChannel()

        try {
            setForeground(
                createForegroundInfo("Menyiapkan upload...")
            )
        } catch (e: Exception) {
            Timber.e(e, "Gagal setForeground")
        }

        Timber.tag("DriveUpload")
            .d("=== DRIVE UPLOAD WORKER STARTED ===")

        // ---------------------------------------------------------
        // 1. AMBIL FOLDER
        // ---------------------------------------------------------

        val folderUriString =
            inputData.getString(KEY_FOLDER_URI)
                ?: return Result.failure()

        val folderUri =
            Uri.parse(folderUriString)

        val folder =
            DocumentFile.fromTreeUri(
                applicationContext,
                folderUri
            ) ?: return Result.failure()

        // ---------------------------------------------------------
        // 2. AMBIL TOKEN
        // ---------------------------------------------------------

        val token =
            preferences.driveAccessToken.first()

        if (token.isBlank()) {

            Timber.e("Access Token kosong")

            showFinalNotification(
                false,
                "Token Google Drive tidak tersedia"
            )

            return Result.failure()
        }

        val drive =
            driveServiceFactory.create(token)

        return try {

            // -----------------------------------------------------
            // 3. BUAT STRUKTUR FOLDER DRIVE
            // -----------------------------------------------------

            val rootName =
                preferences.rootFolder
                    .first()
                    .ifBlank { "My Photo App" }

            val folderLevels =
                preferences.folderLevels.first()

            // ROOT
            val rootId =
                repository.getOrCreateFolder(
                    drive,
                    rootName
                )

            var uploadParentId = rootId

            // SUB FOLDER
            folderLevels
                .map { it.name.trim() }
                .filter { it.isNotBlank() }
                .forEach { folderName ->

                    uploadParentId =
                        repository.getOrCreateFolder(
                            drive,
                            folderName,
                            uploadParentId
                        )
                }

            // PART
            val partFolderId =
                repository.getOrCreateFolder(
                    drive,
                    folder.name ?: "part1",
                    uploadParentId
                )

            Timber.tag("DriveUpload")
                .d("Part Folder ID = $partFolderId")

            // -----------------------------------------------------
            // 4. AMBIL FILE YANG SUDAH ADA DI DRIVE
            // -----------------------------------------------------

            val existingFiles =
                repository.getAllFileNames(
                    drive,
                    partFolderId
                )

            /*
             * ConcurrentHashMap dipakai karena 20 coroutine
             * akan mengakses daftar ini bersamaan.
             */
            val uploadedFiles =
                ConcurrentHashMap.newKeySet<String>()

            uploadedFiles.addAll(existingFiles)

            Timber.tag("DriveUpload")
                .d(
                    "File sudah ada di Drive = ${uploadedFiles.size}"
                )

            // -----------------------------------------------------
            // 5. AMBIL SEMUA FOTO LOKAL (DENGAN STRICT CHECK & SORT)
            // -----------------------------------------------------

            val expectedCount = inputData.getInt(KEY_EXPECTED_COUNT, -1)
            var filesToUpload = folder.listFiles()
                .filter { it.type?.startsWith("image/") == true }
                .sortedBy { it.name?.lowercase() ?: "" }

            if (expectedCount > 0 && filesToUpload.size < expectedCount) {
                Timber.tag("DriveUpload").w("File belum lengkap: ${filesToUpload.size}/$expectedCount. Menunggu...")
                
                var attempts = 0
                while (attempts < 8 && filesToUpload.size < expectedCount) {
                    kotlinx.coroutines.delay(3000) 
                    filesToUpload = folder.listFiles()
                        .filter { it.type?.startsWith("image/") == true }
                        .sortedBy { it.name?.lowercase() ?: "" }
                    attempts++
                    Timber.tag("DriveUpload").d("Retry indexing ke-$attempts: ${filesToUpload.size}/$expectedCount")
                }
                
                if (filesToUpload.size < expectedCount) {
                    Timber.tag("DriveUpload").e("Gagal: File tetap tidak lengkap setelah 8 kali coba.")
                    return Result.retry() 
                }
            }

            val totalFiles = filesToUpload.size

            if (totalFiles == 0) {

                showFinalNotification(
                    true,
                    "Tidak ada foto untuk diupload"
                )

                return Result.success()
            }

            Timber.tag("DriveUpload")
                .d(
                    "Total foto lokal = $totalFiles"
                )

            // -----------------------------------------------------
            // 6. HITUNG YANG SUDAH ADA
            // -----------------------------------------------------

            val alreadyUploaded =
                filesToUpload.count {
                    it.name != null &&
                            uploadedFiles.contains(it.name)
                }

            val pendingFiles =
                filesToUpload.filter {
                    it.name != null &&
                            !uploadedFiles.contains(it.name)
                }

            Timber.tag("DriveUpload")
                .d(
                    "Sudah ada = $alreadyUploaded"
                )

            Timber.tag("DriveUpload")
                .d(
                    "Perlu upload = ${pendingFiles.size}"
                )

            // Semua sudah ada
            if (pendingFiles.isEmpty()) {

                showFinalNotification(
                    true,
                    "Semua foto sudah ada di Google Drive"
                )

                return Result.success()
            }

            // -----------------------------------------------------
            // 7. SEMAPHORE 20 UPLOAD
            // -----------------------------------------------------

            val semaphore =
                Semaphore(UPLOAD_PARALLELISM)

            val completed =
                AtomicInteger(alreadyUploaded)

            val failedFiles =
                ConcurrentHashMap.newKeySet<String>()

            // -----------------------------------------------------
            // 8. UPDATE NOTIFICATION
            // -----------------------------------------------------

            suspend fun updateProgress() {

                val current =
                    completed.incrementAndGet()

                val progressText =
                    "Mengunggah $current/$totalFiles foto"

                try {

                    if (
                        Build.VERSION.SDK_INT >=
                        Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    ) {

                        setForeground(
                            ForegroundInfo(
                                NOTIFICATION_ID,
                                createForegroundInfo(
                                    progressText
                                ).notification,
                                ServiceInfo
                                    .FOREGROUND_SERVICE_TYPE_DATA_SYNC
                            )
                        )

                    } else {

                        setForeground(
                            createForegroundInfo(
                                progressText
                            )
                        )
                    }

                } catch (e: Exception) {

                    Timber.e(
                        e,
                        "Gagal update notification"
                    )
                }
            }

            supervisorScope {
                pendingFiles.map { document ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            if (isStopped) return@withPermit

                            val fileName = document.name ?: "unknown.jpg"

                            // Double check dari cache lokal worker
                            if (uploadedFiles.contains(fileName)) {
                                Timber.tag("DriveUpload").d("SKIP: $fileName")
                                return@withPermit
                            }

                            Timber.tag("DriveUpload").d("START UPLOAD: $fileName")

                            var tempCacheFile: java.io.File? = null
                            try {
                                tempCacheFile = document.copyToCache(applicationContext)

                                repository.uploadFile(
                                    drive = drive,
                                    localFile = tempCacheFile,
                                    parentFolderId = partFolderId
                                )

                                uploadedFiles.add(fileName)
                                updateProgress()
                                Timber.tag("DriveUpload").d("SUCCESS UPLOAD: $fileName")

                            } catch (e: Exception) {
                                failedFiles.add(fileName)
                                Timber.tag("DriveUpload").e(e, "FAILED UPLOAD: $fileName")
                            } finally {
                                try {
                                    tempCacheFile?.delete()
                                } catch (e: Exception) {
                                }
                            }
                        }
                    }
                }.awaitAll()
            }

            // -----------------------------------------------------
            // 10. VERIFIKASI AKHIR: Bandingkan Drive vs Folder Export
            // -----------------------------------------------------

            val finalDriveFiles = repository.getAllFileNames(drive, partFolderId)
            val missingOnDrive = filesToUpload.filter { it.name != null && !finalDriveFiles.contains(it.name) }

            if (missingOnDrive.isNotEmpty()) {
                Timber.tag("DriveUpload").e("VERIFIKASI GAGAL: Ada file di folder export yang belum masuk ke Drive: ${missingOnDrive.map { it.name }}")
                
                showFinalNotification(
                    false,
                    "Gagal: ${missingOnDrive.size} foto tidak terupload sempurna. Mengulang..."
                )
                return Result.retry()
            }

            if (failedFiles.isNotEmpty()) {
                Timber.tag("DriveUpload").e("Upload selesai dengan error: $failedFiles")
                return Result.retry()
            }

            // -----------------------------------------------------
            // 11. SEMUA SELESAI
            // -----------------------------------------------------

            Timber.tag("DriveUpload")
                .d(
                    "=== SEMUA UPLOAD SELESAI ==="
                )

            showFinalNotification(
                true,
                "Upload selesai $totalFiles foto"
            )

            Result.success()

        } catch (e: GoogleJsonResponseException) {

            when (e.statusCode) {

                401 -> {

                    Timber.e(
                        e,
                        "Access Token expired"
                    )

                    val currentToken =
                        preferences.driveAccessToken.first()

                    Timber.tag("DriveToken")
                        .d(
                            "Worker Token = ${
                                token.takeLast(5)
                            }"
                        )

                    Timber.tag("DriveToken")
                        .d(
                            "DataStore Token = ${
                                currentToken.takeLast(5)
                            }"
                        )

                    if (currentToken != token) {

                        Result.retry()

                    } else {

                        showFinalNotification(
                            false,
                            "Token Google Drive sudah kedaluwarsa"
                        )

                        Result.failure()
                    }
                }

                403 -> {

                    showFinalNotification(
                        false,
                        "Permission denied"
                    )

                    Result.failure()
                }

                404 -> {

                    showFinalNotification(
                        false,
                        "Folder tidak ditemukan"
                    )

                    Result.failure()
                }

                else -> {

                    Timber.e(
                        e,
                        "Google Drive error ${e.statusCode}"
                    )

                    Result.retry()
                }
            }

        } catch (e: Exception) {

            Timber.e(
                e,
                "Drive upload error"
            )

            Result.retry()
        }
    }

    // =============================================================
    // FOREGROUND NOTIFICATION
    // =============================================================

    private fun createForegroundInfo(
        progress: String
    ): ForegroundInfo {

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                CHANNEL_ID
            )
                .setContentTitle(
                    "WaterMark Pro - Uploading"
                )
                .setTicker(
                    "Mengunggah ke Google Drive"
                )
                .setContentText(progress)
                .setSmallIcon(
                    android.R.drawable.stat_sys_upload
                )
                .setOngoing(true)
                .build()

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )

        } else {

            ForegroundInfo(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    // =============================================================
    // NOTIFICATION CHANNEL
    // =============================================================

    private fun createNotificationChannel() {

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Google Drive Upload",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    "Status unggah foto ke Google Drive"

                enableLights(true)
                setShowBadge(true)
            }

        notificationManager
            .createNotificationChannel(channel)
    }

    // =============================================================
    // FINAL NOTIFICATION
    // =============================================================

    private fun showFinalNotification(
        success: Boolean,
        message: String
    ) {

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                CHANNEL_ID
            )
                .setContentTitle(
                    if (success)
                        "Upload Selesai"
                    else
                        "Upload Gagal"
                )
                .setContentText(message)
                .setSmallIcon(
                    if (success)
                        android.R.drawable.stat_sys_upload_done
                    else
                        android.R.drawable.stat_notify_error
                )
                .setAutoCancel(false)
                .setOngoing(false)
                .build()

        notificationManager.notify(
            NOTIFICATION_ID + 1,
            notification
        )
    }
}