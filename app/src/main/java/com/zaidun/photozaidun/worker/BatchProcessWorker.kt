package com.zaidun.photozaidun.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.processor.bitmap.BitmapProcessor
import com.zaidun.photozaidun.domain.model.*
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_ACCESS_TOKEN
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_FOLDER_URI
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
@HiltWorker
class BatchProcessWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesDataStore
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Timber.tag("WORKER").d("Worker ID = $id")
        val workerStart = System.currentTimeMillis()
        fun logStep(step: String) {
            Timber.tag("PERF")
                .d("[$step] +${System.currentTimeMillis() - workerStart} ms")
        }
        logStep("Worker Started")
        val semaphore = Semaphore(2)
        try {
            setForeground(createForegroundInfo("Menyiapkan pemrosesan..."))
        } catch (e: Exception) {
            Timber.e("Gagal setForeground: ${e.message}")
        }
        logStep("Foreground Ready")
        val accessToken = inputData.getString(KEY_ACCESS_TOKEN) ?: return Result.failure()
        logStep("Access Token Ready")
        suspend fun enqueueUpload(folder: DocumentFile) {
            val freshToken = preferences.driveAccessToken.first()
            Timber.tag("DriveDebug").d(
                "Enqueue Upload ${folder.name} token=${freshToken.takeLast(5)}"
            )
            val tokenPreview =
                if (freshToken.length > 5) freshToken.takeLast(5) else "EMPTY"
            Timber.tag("DriveDebug")
                .d("Enqueue Upload ${folder.name} token=$tokenPreview")
            val request =
                OneTimeWorkRequestBuilder<DriveUploadWorker>()
                    .addTag("UPLOAD")
                    .setInputData(
                        workDataOf(
                            KEY_FOLDER_URI to folder.uri.toString(),
                            KEY_ACCESS_TOKEN to freshToken
                        )
                    )
                    .build()
            WorkManager.getInstance(applicationContext)
                .enqueue(request)
        }
        Timber.tag("EXPORT").d("BATCH WORKER START")
        val inputFolderUriStr = inputData.getString("input_folder") ?: return Result.failure()
        val inputFolder = DocumentFile.fromTreeUri(applicationContext, Uri.parse(inputFolderUriStr))
            ?: return Result.failure()
        logStep("Input Folder Ready")
        val wmShowTimestamp = preferences.showTimestamp.first()
        val folderLevels =
            preferences.folderLevels.first()
        val partPrefix = preferences.partFolder.first()
        val maxPhotos = preferences.maxPhotoPerFolder.first()
        val suffix = preferences.fileSuffix.first()
        val resize = preferences.resizePercent.first()
        val quality = preferences.jpegQuality.first()
        val wmUri = preferences.watermarkUri.first()
        val wmOpacity = preferences.watermarkOpacity.first()
        val wmScale = preferences.watermarkScale.first()
        val wmPos = preferences.watermarkPosition.first()
        val wmText = preferences.watermarkText.first()
        val wmShowFilename = preferences.showFilename.first()
        val wmShowPart = preferences.showPart.first()
        val wmLogoX = preferences.logoOffsetX.first()
        val wmLogoY = preferences.logoOffsetY.first()
        val wmInfoX = preferences.infoOffsetX.first()
        val wmInfoY = preferences.infoOffsetY.first()
        val wmInfoSize = preferences.infoFontSize.first()
        val startFrom = preferences.startPartNumber.first()
        logStep("Preferences Loaded")
        var outputBaseFolder = inputFolder

        folderLevels
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .forEach { folderName ->

                outputBaseFolder =
                    outputBaseFolder.findFile(folderName)
                        ?: outputBaseFolder.createDirectory(folderName)
                                ?: outputBaseFolder

            }
        val processor = BitmapProcessor(applicationContext)
        val completed = AtomicInteger(0)
        try {
            val listStart = System.currentTimeMillis()
            Timber.tag("PERF").d("Mulai membaca isi folder...")
            val imageFiles = inputFolder.listFiles()
                .filter { it.type?.startsWith("image/") == true }
                .sortedBy { it.name?.lowercase() }
            Timber.tag("PERF").d(
                "listFiles() selesai dalam ${System.currentTimeMillis() - listStart} ms"
            )
            logStep("Image List Loaded")
            kotlinx.coroutines.coroutineScope {
                val parts = imageFiles.chunked(maxPhotos)
                parts.forEachIndexed { partIndex, partFiles ->
                    if (isStopped) return@coroutineScope
                    val partNumber = startFrom + partIndex
                    val currentFolderName = "${partPrefix}$partNumber"
                    val currentOutputFolder = outputBaseFolder.findFile(currentFolderName)
                        ?: outputBaseFolder.createDirectory(currentFolderName)
                    val existingFiles = currentOutputFolder?.listFiles()
                        ?.mapNotNull { it.name }?.toSet() ?: emptySet()
                    Timber.tag("EXPORT").d("Memproses Part $partNumber: ${partFiles.size} foto")
                    val deferredJobs = partFiles.mapIndexed { fileIndexInPart, file ->
                        async {
                            semaphore.withPermit {
                                if (isStopped) return@withPermit
                                val outputFileName = generateNewName(file.name, suffix)
                                if (outputFileName in existingFiles) {
                                    return@withPermit
                                }
                                val outputFile = currentOutputFolder?.createFile(
                                    file.type ?: "image/jpeg",
                                    outputFileName
                                )
                                outputFile?.uri?.let { outUri ->
                                    applicationContext.contentResolver.openOutputStream(outUri)
                                        ?.use { outStream ->
                                            val watermarkConfig = if (wmUri.isNotBlank()) {
                                                WatermarkConfig(
                                                    type = WatermarkType.IMAGE,
                                                    imageUri = Uri.parse(wmUri),
                                                    opacity = wmOpacity,
                                                    size = wmScale,
                                                    showFilename = wmShowFilename,
                                                    position = WatermarkPosition.valueOf(wmPos),
                                                    showPart = wmShowPart,
                                                    logoOffsetX = wmLogoX,
                                                    logoOffsetY = wmLogoY,
                                                    infoOffsetX = wmInfoX,
                                                    infoOffsetY = wmInfoY,
                                                    infoSize = wmInfoSize
                                                )
                                            } else {
                                                WatermarkConfig(
                                                    type = WatermarkType.TEXT,
                                                    text = wmText,
                                                    opacity = wmOpacity,
                                                    size = wmScale,
                                                    position = WatermarkPosition.valueOf(wmPos),
                                                    showFilename = wmShowFilename
                                                )
                                            }
                                            processor.process(
                                                inputUri = file.uri,
                                                watermarkConfig = watermarkConfig,
                                                resizeConfig = ResizeConfig(percentage = resize),
                                                fileName = outputFileName,
                                                partName = currentFolderName,
                                                compressionConfig = CompressionConfig(quality = quality),
                                                outputStream = outStream,
                                                showTimestamp = wmShowTimestamp
                                            )
                                            val done = completed.incrementAndGet()
                                            if (done % 10 == 0 || done == imageFiles.size) {
                                                Timber.tag("EXPORT_PROGRESS").d(
                                                    "done=$done total=${imageFiles.size}"
                                                )
                                                setProgress(
                                                    workDataOf(
                                                        "progress" to (done * 100 / imageFiles.size),
                                                        "current" to done,
                                                        "total" to imageFiles.size,
                                                        "filename" to (file.name ?: "")
                                                    )
                                                )
                                                    Timber.tag("EXPORT_PROGRESS").d("Progress berhasil dikirim")

                                                try {
                                                    val msg =
                                                        "Mengekspor $done / ${imageFiles.size} foto"
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                        setForeground(
                                                            ForegroundInfo(
                                                                102,
                                                                createForegroundInfo(msg).notification,
                                                                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                                                            )
                                                        )
                                                    } else {
                                                        setForeground(createForegroundInfo(msg))
                                                    }
                                                } catch (e: Exception) {
                                                    Timber.e("Gagal update notif: ${e.message}")
                                                }
                                            }
                                        }
                                }
                            }
                        }
                    }

                    deferredJobs.awaitAll()
                    currentOutputFolder?.let { enqueueUpload(it) }
                }
            }
            return Result.success(workDataOf(KEY_OUTPUT_FOLDER to outputBaseFolder.uri.toString()))
        } catch (e: Exception) {
            Timber.e(e, "Gagal dalam BatchProcessWorker")
            return Result.failure()
        } finally {
            processor.clearCache()
        }
    }
    private fun generateNewName(originalName: String?, suffix: String): String {
        val name = originalName ?: "image.jpg"
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex != -1) {
            val baseName = name.substring(0, dotIndex)
            val extension = name.substring(dotIndex)
            if (suffix.isBlank()) baseName + extension else "${baseName}_$suffix$extension"
        } else {
            if (suffix.isBlank()) name else "${name}_$suffix"
        }
    }
    private fun createForegroundInfo(message: String): ForegroundInfo {
        val channelId = "batch_export_channel"

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Batch Export",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Photo Zaidun - Exporting")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                102,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                102,
                notification
            )
        }
    }
}