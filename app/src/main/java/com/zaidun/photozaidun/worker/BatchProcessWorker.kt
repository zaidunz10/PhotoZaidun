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
        val accessToken =
            inputData.getString(KEY_ACCESS_TOKEN)
                ?: return Result.failure()
       fun enqueueUpload(
            folder: DocumentFile,
            accessToken: String
        ) {

        val request =
            OneTimeWorkRequestBuilder<DriveUploadWorker>()
                .addTag("UPLOAD")
                .setInputData(

                    workDataOf(

                        KEY_FOLDER_URI to folder.uri.toString(),

                        KEY_ACCESS_TOKEN to accessToken

                    )

                )
                .build()

        WorkManager
            .getInstance(applicationContext)
            .enqueue(request)

        Timber.tag("UPLOAD")
            .d("UPLOAD ENQUEUE = ${folder.name}")

    }


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
        val wmShowPart = preferences.showPart.first()
        val wmLogoX = preferences.logoOffsetX.first()
        val wmLogoY = preferences.logoOffsetY.first()
        val wmInfoX = preferences.infoOffsetX.first()
        val wmInfoY = preferences.infoOffsetY.first()
        val wmInfoSize = preferences.infoFontSize.first()
        Timber.tag("DriveDr").d("Suffix dari DataStore = '$suffix'")





        val outputBaseFolder = if (mainFolderName.isBlank()) inputFolder else {
            inputFolder.findFile(mainFolderName) ?: inputFolder.createDirectory(mainFolderName) ?: inputFolder
        }

        val processor = BitmapProcessor(applicationContext)

        val imageFiles = inputFolder.listFiles()
            .filter { it.type?.startsWith("image/") == true }
            .sortedBy { it.name?.lowercase() }

        var photoCount = 0
        var currentPart = 1
        var currentOutputFolder = outputBaseFolder.findFile("${partPrefix}$currentPart")
            ?: outputBaseFolder.createDirectory("${partPrefix}$currentPart")

        imageFiles.forEachIndexed { index, file ->
            if (isStopped) {
                Timber.tag("EXPORT").d("Worker dibatalkan")
                return Result.success()
            }
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
                if (photoCount >= maxPhotos) {

                    currentOutputFolder?.let {

                        enqueueUpload(
    it,
    accessToken
)

                    }

                    currentPart++

                    currentOutputFolder =
                        outputBaseFolder.findFile("${partPrefix}$currentPart")
                            ?: outputBaseFolder.createDirectory("${partPrefix}$currentPart")

                    photoCount = 0
                }
                val outputFile = currentOutputFolder?.createFile(
                    file.type ?: "image/jpeg",
                    newName
                )
                outputFile?.uri?.let { outUri ->

                    applicationContext.contentResolver.openOutputStream(outUri)?.use { outStream ->
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
                        if (isStopped) {
                            Timber.d("Worker dibatalkan setelah proses bitmap")
                            return Result.success()
                        }
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

        currentOutputFolder?.let {

            enqueueUpload(
    it,
    accessToken
)

        }
        return Result.success(
            workDataOf(
                KEY_OUTPUT_FOLDER to outputBaseFolder.uri.toString()
            )
        )
    }


}
