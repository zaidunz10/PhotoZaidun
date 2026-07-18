package com.zaidun.photozaidun.worker

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

    override suspend fun doWork(): Result {

        // Ambil URI folder yang dikirim dari BatchProcessWorker
        val folderUriStr = inputData.getString("folder_uri") ?: return Result.failure()
        val folderUri = Uri.parse(folderUriStr)

        val token = preferences.driveAccessToken.first()
        Timber.d("Drive Token = ${token.take(20)}...")
        if (token.isBlank()) {
            Timber.e("Access Token kosong")
            return Result.failure()
        }
        val drive = driveServiceFactory.create(token)


        // Memerlukan import androidx.documentfile.provider.DocumentFile
        val folder = DocumentFile.fromTreeUri(applicationContext, folderUri) ?: return Result.failure()

        return try {
            val rootFolderId = repository.getOrCreateFolder(
                drive = drive,
                folderName = "Photo Zaidun"
            )
            Timber.d("Root Folder : $rootFolderId")

            val partFolderId = repository.getOrCreateFolder(
                drive = drive,
                folderName = folder.name ?: "part_1",
                parentId = rootFolderId
            )
            Timber.d("Part Folder : $partFolderId")

            Timber.d("Memulai upload folder ke Google Drive: ${folder.name}")

            val filesToUpload = folder.listFiles().filter {
                it.type?.startsWith("image/") == true
            }


            filesToUpload.forEach { document ->

                val tempFile =
                    document.copyToCache(applicationContext)

                try {

                    repository.uploadFile(
                        drive = drive,
                        localFile = tempFile,
                        parentFolderId = partFolderId
                    )

                    Timber.d("${document.name} berhasil diupload")

                } finally {

                    tempFile.delete()

                }

            }

            Result.success()
        } catch (e: Exception) {

            Timber.e(e)

            return if (
                e.message?.contains("401") == true ||
                e.message?.contains("403") == true
            ) {
                Result.failure()
            } else {
                Result.retry()
            }

        }
    }
}