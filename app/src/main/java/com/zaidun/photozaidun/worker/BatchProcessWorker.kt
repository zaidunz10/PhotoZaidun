package com.zaidun.photozaidun.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.common.collect.Multimaps.index
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.processor.bitmap.BitmapProcessor
import com.zaidun.photozaidun.domain.model.*
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
        Timber.tag("EXPORT").d("BATCH WORKER START")
        val inputFolderUriStr = inputData.getString("input_folder") ?: return Result.failure()
        val inputFolder = DocumentFile.fromTreeUri(applicationContext, Uri.parse(inputFolderUriStr)) ?: return Result.failure()

        // 1. Ambil Pengaturan dari DataStore
        val mainFolderName = preferences.mainFolder.first() // Misal: "Wedding JONAS"
        val partPrefix = preferences.partFolder.first()      // Missal: "Part"
        val maxPhotos = preferences.maxPhotoPerFolder.first() // Missal: 200

        val wmUri = preferences.watermarkUri.first()
        val wmOpacity = preferences.watermarkOpacity.first()
        val wmScale = preferences.watermarkScale.first()
        val wmPos = preferences.watermarkPosition.first()
        val wmText = preferences.watermarkText.first()
        val wmShowFilename = preferences.showFilename.first()
        val resize = preferences.resizePercent.first()
        val quality = preferences.jpegQuality.first()
        val suffix = preferences.fileSuffix.first()
        Timber.tag("DriveDr").d("Suffix dari DataStore = '$suffix'")





        // 2. Siapkan Folder Output Utama di HP (Lokal)
        // Kita buat folder hasil di dalam folder input agar mudah ditemukan
        val outputBaseFolder = inputFolder.createDirectory(mainFolderName) ?: inputFolder

        val processor = BitmapProcessor(applicationContext)
        val imageFiles = inputFolder.listFiles().filter {
            it.type?.startsWith("image/") == true
        }

        var photoCount = 0
        var currentPart = 1
        var currentOutputFolder = outputBaseFolder.createDirectory("${partPrefix}$currentPart")

        imageFiles.forEachIndexed { index, file ->
            val originalName = file.name ?: "image.jpg"

            val dotIndex = originalName.lastIndexOf('.')

            val newName = if (dotIndex != -1) {
                val baseName = originalName.substring(0, dotIndex)
                val extension = originalName.substring(dotIndex)

                if (suffix.isBlank()) {
                    baseName + extension
                } else {
                    "${baseName}_$suffix$extension"
                }
            } else {
                if (suffix.isBlank()) {
                    originalName
                } else {
                    "${originalName}_$suffix"
                }
            }
            try {
                // Logic Splitting Folder V2
                if (photoCount >= maxPhotos) {
                    // Trigger Upload Folder Part yang sudah penuh ke Drive jika Auto Upload aktif
                    if (preferences.autoUpload.first()) {
                        triggerDriveUpload(currentOutputFolder?.uri, mainFolderName)
                    }

                    currentPart++
                    currentOutputFolder = outputBaseFolder.createDirectory("${partPrefix}$currentPart")
                    photoCount = 0
                }
                val outputFile = currentOutputFolder?.createFile(
                    file.type ?: "image/jpeg",
                    newName
                )
                outputFile?.uri?.let { outUri ->
                    if (isStopped) {
                        return Result.failure()
                    }
                    applicationContext.contentResolver.openOutputStream(outUri)?.use { outStream ->
                        val watermarkConfig = if (wmUri.isNotBlank()) {
                            WatermarkConfig(
                                type = WatermarkType.IMAGE,
                                imageUri = Uri.parse(wmUri),
                                opacity = wmOpacity,
                                size = wmScale,
                                showFilename = wmShowFilename,
                                position = WatermarkPosition.valueOf(wmPos)
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


                        // Di dalam imageFiles.forEach { file -> ... }
                        processor.process(
                            inputUri = file.uri,
                            watermarkConfig = watermarkConfig,
                            resizeConfig = ResizeConfig(percentage = resize),
                            fileName = newName,
                            partName = "${partPrefix}$currentPart",
                            compressionConfig = CompressionConfig(quality = quality),
                            outputStream = outStream
                        )
                        setProgress(
                            workDataOf(
                                "progress" to ((index + 1) * 100 / imageFiles.size),
                                "current" to (index + 1),
                                "total" to imageFiles.size,
                                "filename" to (file.name ?: "")
                            )
                        )
                    }
                }
                photoCount++
            } catch (e: Exception) {
                Timber.e(e, "Gagal memproses file: ${file.name}")
            }
        }

        // Upload Part Terakhir
        if (preferences.autoUpload.first()) {
            triggerDriveUpload(currentOutputFolder?.uri, mainFolderName)
        }

        return Result.success()
    }

    private fun triggerDriveUpload(folderUri: Uri?, driveParentFolder: String) {
        if (folderUri == null) return
        val uploadRequest = OneTimeWorkRequestBuilder<DriveUploadWorker>()
            .setInputData(workDataOf(
                "folder_uri" to folderUri.toString(),
                "drive_folder_name" to driveParentFolder
            ))
            .build()
        WorkManager.getInstance(applicationContext).enqueue(uploadRequest)
    }
}
