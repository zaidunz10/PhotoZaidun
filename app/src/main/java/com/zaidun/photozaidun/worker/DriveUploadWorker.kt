package com.zaidun.photozaidun.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import timber.log.Timber

class DriveUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Ambil URI folder yang dikirim dari BatchProcessWorker
        val folderUriStr = inputData.getString("folder_uri") ?: return Result.failure()
        val folderUri = Uri.parse(folderUriStr)

        // Memerlukan import androidx.documentfile.provider.DocumentFile
        val folder = DocumentFile.fromTreeUri(applicationContext, folderUri) ?: return Result.failure()

        return try {
            Timber.d("Memulai upload folder ke Google Drive: ${folder.name}")

            // Ambil semua file hasil proses (yang diawali 'zaidun_')
            val filesToUpload = folder.listFiles().filter { it.name?.startsWith("zaidun_") == true }

            filesToUpload.forEach { file ->
                // LOGIKA DRIVE API v3 DISINI
                // Untuk sementara kita log dulu namanya
                Timber.d("Sedang mengupload ke Drive: ${file.name}")


            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Gagal upload ke Drive, mencoba lagi...")
            Result.retry()
        }
    }
}