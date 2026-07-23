package com.zaidun.photozaidun.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
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
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.processor.bitmap.BitmapProcessor
import com.zaidun.photozaidun.domain.model.*
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_ACCESS_TOKEN
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_FOLDER_URI
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber

@HiltWorker
class BatchProcessWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        // 1. JADIKAN FOREGROUND SEGERA
        try {
            setForeground(createForegroundInfo("Menyiapkan pemrosesan..."))
        } catch (e: Exception) {
            Timber.e("Gagal setForeground: ${e.message}")
        }

        val accessToken = inputData.getString(KEY_ACCESS_TOKEN) ?: return Result.failure()

        fun enqueueUpload(folder: DocumentFile) {
            val request = OneTimeWorkRequestBuilder<DriveUploadWorker>()
                .addTag("UPLOAD")
                .setInputData(
                    workDataOf(
                        KEY_FOLDER_URI to folder.uri.toString(),
                        KEY_ACCESS_TOKEN to accessToken
                    )
                )
                .build()
            WorkManager.getInstance(applicationContext).enqueue(request)
        }

        Timber.tag("EXPORT").d("BATCH WORKER START")
        val inputFolderUriStr = inputData.getString("input_folder") ?: return Result.failure()
        val inputFolder = DocumentFile.fromTreeUri(applicationContext, Uri.parse(inputFolderUriStr)) ?: return Result.failure()

        // Ambil Pengaturan
        val wmShowTimestamp = preferences.showTimestamp.first()
        val mainFolderName = preferences.mainFolder.first()
        val partPrefix = preferences.partFolder.first()
        val maxPhotos = preferences.maxPhotoPerFolder.first()
        val suffix = preferences.fileSuffix.first()
        val resize = preferences.resizePercent.first()
        val quality = preferences.jpegQuality.first()

        // Preferensi Watermark
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

        val outputBaseFolder = if (mainFolderName.isBlank()) inputFolder else {
            inputFolder.findFile(mainFolderName) ?: inputFolder.createDirectory(mainFolderName) ?: inputFolder
        }

        val processor = BitmapProcessor(applicationContext)
        try {
            val imageFiles = inputFolder.listFiles()
                .filter { it.type?.startsWith("image/") == true }
                .sortedBy { it.name?.lowercase() }

            var photoCount = 0
            var currentPart = startFrom
            var currentOutputFolder = outputBaseFolder.findFile("${partPrefix}$currentPart")
                ?: outputBaseFolder.createDirectory("${partPrefix}$currentPart")


            imageFiles.forEachIndexed { index, file ->
                val outputFileName = generateNewName(file.name, suffix)

                // LOGIKA RESUME
                if (currentOutputFolder?.findFile(outputFileName) != null) {
                    photoCount++
                    return@forEachIndexed
                }

                if (isStopped) return Result.failure()
                setForeground(createForegroundInfo("Memproses $index / ${imageFiles.size}: ${file.name}"))

                try {
                    if (photoCount >= maxPhotos) {
                        currentOutputFolder?.let { enqueueUpload(it) }
                        currentPart++
                        currentOutputFolder = outputBaseFolder.findFile("${partPrefix}$currentPart")
                            ?: outputBaseFolder.createDirectory("${partPrefix}$currentPart")
                        photoCount = 0
                    }

                    val outputFile = currentOutputFolder?.createFile(file.type ?: "image/jpeg", outputFileName)
                    outputFile?.uri?.let { outUri ->
                        applicationContext.contentResolver.openOutputStream(outUri)?.use { outStream ->

                            // DEFINISIKAN WATERMARK CONFIG DI SINI
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
                                partName = "${partPrefix}$currentPart",
                                exifDate = "",
                                compressionConfig = CompressionConfig(quality = quality),
                                outputStream = outStream,
                                showTimestamp = wmShowTimestamp
                            )

                            setProgress(workDataOf(
                                "progress" to ((index + 1) * 100 / imageFiles.size),
                                "current" to (index + 1),
                                "total" to imageFiles.size,
                                "filename" to (file.name ?: "")
                            ))
                        }
                    }
                    photoCount++
                } catch (e: Exception) {
                    Timber.e(e, "Gagal memproses file: ${file.name}")
                }
            }
            currentOutputFolder?.let { enqueueUpload(it) }
            return Result.success(workDataOf(KEY_OUTPUT_FOLDER to outputBaseFolder.uri.toString()))
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
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Batch Export", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Photo Zaidun - Exporting")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        return ForegroundInfo(102, notification)
    }
}