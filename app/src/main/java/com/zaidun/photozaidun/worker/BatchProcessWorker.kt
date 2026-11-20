package com.zaidun.photozaidun.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.processor.bitmap.BitmapProcessor
import com.zaidun.photozaidun.data.remote.UserLogger
import com.zaidun.photozaidun.data.scanner.FastScanner
import com.zaidun.photozaidun.data.scanner.ScanPhoto
import com.zaidun.photozaidun.domain.model.*
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_ACCESS_TOKEN
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_FOLDER_URI
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.BufferedOutputStream
import java.util.Calendar
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

@HiltWorker
class BatchProcessWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesDataStore,
) : CoroutineWorker(context, params) {
    private val outputFileLock = Any()

    override suspend fun doWork(): Result {
        Timber.tag("WORKER").d("Worker ID = $id")
        val workerStart = System.currentTimeMillis()
        fun logStep(step: String) {
            Timber.tag("PERF")
                .d("[$step] +${System.currentTimeMillis() - workerStart} ms")
        }
        logStep("Worker Started")

        val parallelism = (Runtime.getRuntime().availableProcessors()).coerceIn(2, 4)
        val semaphore = Semaphore(parallelism)
        try {
            setForeground(createForegroundInfo("Menyiapkan pemrosesan..."))
        } catch (e: Exception) {
            Timber.e("Gagal setForeground: ${e.message}")
        }
        logStep("Foreground Ready")
        val accessToken = inputData.getString(KEY_ACCESS_TOKEN) ?: return Result.failure()
        logStep("Access Token Ready")

        suspend fun enqueueUpload(folder: DocumentFile, expectedCount: Int, hourFolder: String) {
            val freshToken = preferences.driveAccessToken.first()
            Timber.tag("DriveDebug").d(
                "Enqueue Upload ${folder.name} hour=$hourFolder count=$expectedCount token=${freshToken.takeLast(5)}"
            )
            val tokenPreview =
                if (freshToken.length > 5) freshToken.takeLast(5) else "EMPTY"
            Timber.tag("DriveDebug")
                .d("Enqueue Upload ${folder.name} token=$tokenPreview")
            val sessionId = inputData.getLong("session_id", -1L)
            val isPublished = inputData.getBoolean("is_published", true)
            val locationName = inputData.getString("location") ?: ""
            val request =
                OneTimeWorkRequestBuilder<DriveUploadWorker>()
                    .addTag("UPLOAD")
                    .setInputData(
                        workDataOf(
                            KEY_FOLDER_URI to folder.uri.toString(),
                            KEY_ACCESS_TOKEN to freshToken,
                            DriveUploadWorker.KEY_EXPECTED_COUNT to expectedCount,
                            "session_id" to sessionId,
                            "is_published" to isPublished,
                            "location" to locationName,
                            DriveUploadWorker.KEY_HOUR_FOLDER to hourFolder
                        )
                    )
                    .build()
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    "UPLOAD_${hourFolder}_${folder.name}",
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request
                )
        }

        Timber.tag("EXPORT").d("BATCH WORKER START")
        val inputFolderUriStr = inputData.getString("input_folder") ?: return Result.failure()
        val inputFolder = DocumentFile.fromTreeUri(applicationContext, Uri.parse(inputFolderUriStr))
            ?: return Result.failure()
        logStep("Input Folder Ready")

        val wmShowTimestamp = preferences.showTimestamp.first()
        val folderLevels = preferences.folderLevels.first()
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

        // Pool BitmapProcessor agar aman saat dijalankan paralel oleh semaphore
        val processorPool = ConcurrentLinkedQueue<BitmapProcessor>()
        val createdProcessors = ConcurrentLinkedQueue<BitmapProcessor>()
        fun acquireProcessor(): BitmapProcessor {
            return processorPool.poll() ?: BitmapProcessor(applicationContext).also {
                createdProcessors.add(it)
            }
        }

        val completed = AtomicInteger(0)
        val watermarkConfig =
            if (wmUri.isNotBlank()) {
                val logoUri = Uri.parse(wmUri)
                val position = WatermarkPosition.valueOf(wmPos)
                WatermarkConfig(
                    type = WatermarkType.IMAGE,
                    imageUri = logoUri,
                    opacity = wmOpacity,
                    size = wmScale,
                    showFilename = wmShowFilename,
                    position = position,
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

        try {
            val listStart = System.currentTimeMillis()
            Timber.tag("PERF").d("Mulai membaca isi folder...")
            val scanStart = System.currentTimeMillis()

            val imageFiles =
                FastScanner.scan(
                    applicationContext,
                    inputFolder
                ).images.sortedBy { it.name?.lowercase() ?: "" }

            Timber.tag("SCAN")
                .d(
                    "Scanner selesai ${imageFiles.size} foto dalam ${
                        System.currentTimeMillis() - scanStart
                    } ms"
                )
            Timber.tag("PERF").d(
                "listFiles() selesai dalam ${System.currentTimeMillis() - listStart} ms"
            )
            logStep("Image List Loaded")

            // Catat log export ke Firebase Firestore secara asinkron (0 ms blocking)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val email = preferences.userEmail.first()
                    val name = preferences.userName.first()
                    UserLogger.logExport(
                        email.ifBlank { "Guest" },
                        name.ifBlank { "Guest User" },
                        imageFiles.size
                    )
                } catch (e: Exception) {
                    Timber.w(e, "Gagal mencatat log Firestore")
                }
            }

            val groupedByHour = coroutineScope {
                imageFiles.map { file ->
                    async(Dispatchers.IO) {
                        file to getExifHourRange(file)
                    }
                }.awaitAll()
            }.groupBy({ it.second }, { it.first })

            coroutineScope {
                groupedByHour.forEach { (hourFolder, hourFiles) ->
                    if (isStopped) return@coroutineScope
                    val hourOutputFolder = outputBaseFolder.findFile(hourFolder)
                        ?: outputBaseFolder.createDirectory(hourFolder)

                    Timber.tag("EXPORT").d("Memproses Jam $hourFolder: ${hourFiles.size} foto")

                    if (wmShowPart) {
                        // JALUR 1: PART AKTIF -> Bagi per maxPhotos, selesai Part 1 langsung upload Part 1, dst.
                        val partChunks = hourFiles.chunked(maxPhotos.coerceAtLeast(1))
                        partChunks.forEachIndexed { partIdx, partFiles ->
                            if (isStopped) return@coroutineScope

                            val partNum = startFrom + partIdx
                            val partFolderName = "part $partNum"
                            val currentOutputFolder = hourOutputFolder?.findFile(partFolderName)
                                ?: hourOutputFolder?.createDirectory(partFolderName)

                            val existingFiles = HashSet<String>()
                            currentOutputFolder
                                ?.listFiles()
                                ?.forEach { it.name?.let(existingFiles::add) }

                            partFiles.map { file ->
                                async(Dispatchers.IO) {
                                    semaphore.withPermit {
                                        if (isStopped) return@withPermit

                                        var alreadyExists = false
                                        var outputFileName = ""
                                        var outputFile: DocumentFile? = null

                                        synchronized(outputFileLock) {
                                            val generatedName = generateNewName(file.name, suffix)
                                            if (generatedName in existingFiles) {
                                                alreadyExists = true
                                            } else {
                                                existingFiles.add(generatedName)
                                                outputFile = currentOutputFolder?.createFile(file.mime, generatedName)
                                                outputFileName = generatedName
                                            }
                                        }

                                        if (alreadyExists) {
                                            val done = completed.incrementAndGet()
                                            if (done % 5 == 0 || done == imageFiles.size) {
                                                updateExportProgress(done, imageFiles.size, file.name ?: "")
                                            }
                                            return@withPermit
                                        }

                                        val processor = acquireProcessor()
                                        try {
                                            outputFile?.uri?.let { outUri ->
                                                applicationContext.contentResolver.openOutputStream(outUri)?.use { rawStream ->
                                                    BufferedOutputStream(rawStream, 65536).use { outStream ->
                                                        processor.process(
                                                            inputUri = file.uri,
                                                            watermarkConfig = watermarkConfig,
                                                            resizeConfig = ResizeConfig(percentage = resize),
                                                            fileName = outputFileName,
                                                            partName = "$hourFolder / $partFolderName",
                                                            compressionConfig = CompressionConfig(quality = quality),
                                                            outputStream = outStream,
                                                            showTimestamp = wmShowTimestamp
                                                        )
                                                    }
                                                }

                                                val done = completed.incrementAndGet()
                                                if (done % 5 == 0 || done == imageFiles.size) {
                                                    updateExportProgress(done, imageFiles.size, file.name ?: "")
                                                }
                                            }
                                        } finally {
                                            processorPool.offer(processor)
                                        }
                                    }
                                }
                            }.awaitAll()

                            // Selesai 1 Part -> langsung picu upload untuk Part ini!
                            currentOutputFolder?.let { enqueueUpload(it, partFiles.size, hourFolder) }
                        }
                    } else {
                        // JALUR 2: PART MATI -> Pemrosesan dibagi per-batch 500 foto di dalam folder jam
                        val existingFiles = HashSet<String>()
                        hourOutputFolder
                            ?.listFiles()
                            ?.forEach { it.name?.let(existingFiles::add) }

                        val batches = hourFiles.chunked(500)
                        var cumulativeCount = 0

                        batches.forEach { batchFiles ->
                            if (isStopped) return@coroutineScope

                            batchFiles.map { file ->
                                async(Dispatchers.IO) {
                                    semaphore.withPermit {
                                        if (isStopped) return@withPermit

                                        var alreadyExists = false
                                        var outputFileName = ""
                                        var outputFile: DocumentFile? = null

                                        synchronized(outputFileLock) {
                                            val generatedName = generateNewName(file.name, suffix)
                                            if (generatedName in existingFiles) {
                                                alreadyExists = true
                                            } else {
                                                existingFiles.add(generatedName)
                                                outputFile = hourOutputFolder?.createFile(file.mime, generatedName)
                                                outputFileName = generatedName
                                            }
                                        }

                                        if (alreadyExists) {
                                            val done = completed.incrementAndGet()
                                            if (done % 5 == 0 || done == imageFiles.size) {
                                                updateExportProgress(done, imageFiles.size, file.name ?: "")
                                            }
                                            return@withPermit
                                        }

                                        val processor = acquireProcessor()
                                        try {
                                            outputFile?.uri?.let { outUri ->
                                                applicationContext.contentResolver.openOutputStream(outUri)?.use { rawStream ->
                                                    BufferedOutputStream(rawStream, 65536).use { outStream ->
                                                        processor.process(
                                                            inputUri = file.uri,
                                                            watermarkConfig = watermarkConfig,
                                                            resizeConfig = ResizeConfig(percentage = resize),
                                                            fileName = outputFileName,
                                                            partName = hourFolder,
                                                            compressionConfig = CompressionConfig(quality = quality),
                                                            outputStream = outStream,
                                                            showTimestamp = wmShowTimestamp
                                                        )
                                                    }
                                                }

                                                val done = completed.incrementAndGet()
                                                if (done % 5 == 0 || done == imageFiles.size) {
                                                    updateExportProgress(done, imageFiles.size, file.name ?: "")
                                                }
                                            }
                                        } finally {
                                            processorPool.offer(processor)
                                        }
                                    }
                                }
                            }.awaitAll()

                            cumulativeCount += batchFiles.size
                            // Sesaat setelah 500 foto selesai diekspor di HP, langsung picu upload!
                            hourOutputFolder?.let { enqueueUpload(it, cumulativeCount, hourFolder) }
                        }
                    }
                }
            }
            return Result.success(workDataOf(KEY_OUTPUT_FOLDER to outputBaseFolder.uri.toString()))
        } catch (e: Exception) {
            Timber.e(e, "Gagal dalam BatchProcessWorker")
            return Result.failure()
        } finally {
            createdProcessors.forEach {
                try {
                    it.clearCache()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun getExifHourRange(photo: ScanPhoto): String {
        var hour: Int? = null

        // 1. UTAMA (100% AKURAT): Baca Tanggal & Jam Asli Kamera dari EXIF Header JPEG!
        try {
            applicationContext.contentResolver.openInputStream(photo.uri)?.use { input ->
                val exif = ExifInterface(input)
                val dateStr = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

                if (!dateStr.isNullOrBlank()) {
                    val timePart = when {
                        dateStr.contains(' ') -> dateStr.substringAfter(' ')
                        dateStr.contains('T') -> dateStr.substringAfter('T')
                        else -> dateStr
                    }
                    val hourString = timePart.substringBefore(':').trim()
                    hour = hourString.toIntOrNull()
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Gagal membaca EXIF date")
        }

        // 2. FALLBACK JIKA FOTO TIDAK MEMILIKI DATETIME EXIF: Gunakan lastModified
        if (hour == null || hour !in 0..23) {
            if (photo.lastModified > 0L) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = photo.lastModified
                hour = cal.get(Calendar.HOUR_OF_DAY)
            }
        }

        // 3. FALLBACK TERAKHIR: Gunakan jam sistem saat ini
        if (hour == null || hour !in 0..23) {
            val cal = Calendar.getInstance()
            hour = cal.get(Calendar.HOUR_OF_DAY)
        }

        val startHour = String.format("%02d.00", hour)
        val endHour = String.format("%02d.00", (hour + 1) % 24)
        return "$startHour - $endHour"
    }

    private suspend fun updateExportProgress(done: Int, total: Int, fileName: String) {
        setProgress(
            workDataOf(
                "progress" to (done * 100 / total),
                "current" to done,
                "total" to total,
                "filename" to fileName
            )
        )
        try {
            val msg = "Mengekspor $done / $total foto"
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