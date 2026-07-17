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
import androidx.core.net.toUri
import com.zaidun.photozaidun.data.processor.BitmapProcessor
import com.zaidun.photozaidun.domain.model.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import kotlinx.coroutines.flow.first
@HiltWorker
class BatchProcessWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesDataStore

) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val inputFolderStr = inputData.getString("input_folder") ?: return Result.failure()
        val inputFolderUri = Uri.parse(inputFolderStr)
        val inputFolder =
            DocumentFile.fromTreeUri(applicationContext, inputFolderUri) ?: return Result.failure()

        val processor = BitmapProcessor(applicationContext)
        val wmUri = preferences.watermarkUri.first()

        val opacity = preferences.watermarkOpacity.first()

        val scale = preferences.watermarkScale.first()

        val position = preferences.watermarkPosition.first()

        val resize = preferences.resizePercent.first()

        val quality = preferences.jpegQuality.first()
        val imageFiles = inputFolder.listFiles().filter {
            it.type?.startsWith("image/") == true && it.name?.startsWith("zaidun_") == false
        }

        var photoCount = 0
        var currentPart = 1
        var currentOutputFolder = inputFolder.createDirectory("Part_$currentPart")

        imageFiles.forEach { file ->
            try {
                if (photoCount >= 200) {
                    // FOLDER LAMA SELESAI (200 Foto) - SIAP UPLOAD
                    val folderToUpload = currentOutputFolder?.uri
                    triggerDriveUpload(folderToUpload) // Fungsi upload otomatis

                    currentPart++
                    currentOutputFolder = inputFolder.createDirectory("Part_$currentPart")
                    photoCount = 0
                }

                val outputFile = currentOutputFolder?.createFile(
                    file.type ?: "image/jpeg",
                    "zaidun_${file.name}"
                )
                outputFile?.uri?.let { outUri ->
                    applicationContext.contentResolver.openOutputStream(outUri)?.use { outStream ->
                        val watermarkConfig =
                            if (wmUri.isNotBlank()) {

                                WatermarkConfig(
                                    type = WatermarkType.IMAGE,
                                    imageUri = Uri.parse(wmUri),
                                    opacity = opacity,
                                    size = scale,
                                    position = WatermarkPosition.valueOf(position)
                                )

                            } else {

                                WatermarkConfig(
                                    text = "Photo Zaidun"
                                )

                            }

                        processor.process(

                            file.uri,

                            watermarkConfig,

                            ResizeConfig(
                                percentage = resize
                            ),

                            CompressionConfig(
                                quality = quality
                            ),

                            outStream

                        )
                    }
                }
                photoCount++
            } catch (e: Exception) { /* Log error */
            }
        }
        return Result.success()
    }

    // Fungsi pembantu untuk trigger worker upload
    private fun triggerDriveUpload(folderUri: Uri?) {
        if (folderUri == null) return

        val uploadRequest = OneTimeWorkRequestBuilder<DriveUploadWorker>()
            .setInputData(workDataOf("folder_uri" to folderUri.toString()))
            .addTag("UPLOAD_DRIVE")
            .build()

        WorkManager.getInstance(applicationContext).enqueue(uploadRequest)
        Timber.d("Folder Part telah penuh (200 foto). Antrean upload Drive dimulai.")
    }
}