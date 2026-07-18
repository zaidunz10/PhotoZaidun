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
        val maxPhoto = preferences.maxPhotoPerFolder.first()
        val suffix = preferences.fileSuffix.first()
        val partPrefix = preferences.partFolder.first()

        val resize = preferences.resizePercent.first()
        val quality = preferences.jpegQuality.first()
        val inputFolderStr = inputData.getString("input_folder") ?: return Result.failure()
        val inputFolderUri = Uri.parse(inputFolderStr)
        val inputFolder =
            DocumentFile.fromTreeUri(applicationContext, inputFolderUri) ?: return Result.failure()

        val processor = BitmapProcessor(applicationContext)
        val wmUri = preferences.watermarkUri.first().takeIf { it.isNotBlank() }?.let(Uri::parse)

        val opacity = preferences.watermarkOpacity.first()

        val scale = preferences.watermarkScale.first()

        val position = preferences.watermarkPosition.first()


        val imageFiles = inputFolder.listFiles().filter { file ->
            val isImage = file.type?.startsWith("image/") == true

            // Jika user input "zaidunz", maka file yang sudah ada nama "zaidunz" akan diabaikan
            val isAlreadyProcessed = suffix.isNotBlank() && file.name?.contains(suffix) == true

            isImage && !isAlreadyProcessed
        }

        // 3. Ambil Nama Folder Utama dari Settings
        val mainFolderName = preferences.mainFolder.first()

        // Folder induk untuk part-part adalah folder utama, jika kosong gunakan inputFolder
        val targetParentFolder = if (mainFolderName.isNotBlank()) {
            inputFolder.findFile(mainFolderName) ?: inputFolder.createDirectory(mainFolderName) ?: inputFolder
        } else {
            inputFolder
        }
        var photoCount = 0
        var currentPart = 1
        var currentOutputFolder =
            targetParentFolder.createDirectory(
                "${partPrefix}_$currentPart"
            ) ?: return Result.failure()
        val autoUpload = preferences.autoUpload.first()


        imageFiles.forEach { file ->
            try {
                if (photoCount >= maxPhoto) {
                    val folderToUpload = currentOutputFolder?.uri

                    if (autoUpload) {
                        triggerDriveUpload(folderToUpload)
                    }

                    currentPart++

                    currentOutputFolder =
                        targetParentFolder.createDirectory(
                            "${partPrefix}_$currentPart"
                        ) ?: return Result.failure()
                    photoCount = 0
                }

                // 1. Definisikan newName di luar blok IF agar bisa diakses di bawah
                val originalName = file.name ?: "image"
                val newName = if (suffix.isNotBlank()) {
                    val baseName = originalName.substringBeforeLast(".")
                    val extension = originalName.substringAfterLast(".", "jpg")
                    "${baseName}_${suffix}.$extension"
                } else {
                    originalName
                }

                val outputFile = currentOutputFolder?.createFile(
                    file.type ?: "image/jpeg",
                    newName
                )

                // 2. Jalankan proses Bitmap
                outputFile?.uri?.let { outUri ->
                    applicationContext.contentResolver.openOutputStream(outUri)?.use { outStream ->

                        val watermarkPosition = try {
                            WatermarkPosition.valueOf(position)
                        } catch (e: Exception) {
                            WatermarkPosition.BOTTOM_RIGHT
                        }
                        processor.process(
                            file.uri,
                            WatermarkConfig(
                                type = WatermarkType.IMAGE,
                                imageUri = wmUri,
                                text = "",
                                opacity = opacity,
                                size = scale,
                                position = watermarkPosition,
                                marginX = 0.02f,
                                marginY = 0.02f
                            ),
                            ResizeConfig(percentage = resize),
                            CompressionConfig(quality = quality),
                            outStream
                        )
                    }
                }

                photoCount++ // Jangan lupa tambah count
            } catch (e: Exception) {
                Timber.e(e, "Gagal memproses file: ${file.name}")
            }
        }

        // --- TRIGGER UNTUK PART TERAKHIR (SISANYA) ---
        // Hapus "com.zaidun.photozaidun.worker." karena ini variabel lokal
        if (autoUpload && photoCount > 0) {
            triggerDriveUpload(currentOutputFolder?.uri)
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
        Timber.d(
            "Folder Part telah penuh foto"
        )
    }
}