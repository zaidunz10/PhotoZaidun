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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.zaidun.photozaidun.data.local.dao.PhotoAiMetadataDao
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.local.entity.PhotoAiMetadataEntity
import com.zaidun.photozaidun.data.processor.bitmap.BitmapProcessor
import com.zaidun.photozaidun.data.remote.UserLogger
import com.zaidun.photozaidun.data.remote.gemini.GeminiKeyManager
import com.zaidun.photozaidun.data.remote.gemini.GeminiPhotoAnalyzer
import com.zaidun.photozaidun.data.scanner.FastScanner
import com.zaidun.photozaidun.data.scanner.ScanPhoto
import com.zaidun.photozaidun.domain.model.*
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_ACCESS_TOKEN
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_FOLDER_URI
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.BufferedOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber

private data class ProcessItem(
    val file: ScanPhoto,
    val outputFileName: String,
    val outputFile: DocumentFile?,
    val processor: BitmapProcessor,
    val resizedBitmap: Bitmap
)

@HiltWorker
class BatchProcessWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: UserPreferencesDataStore,
    private val geminiAnalyzer: GeminiPhotoAnalyzer,
    private val geminiKeyManager: GeminiKeyManager,
    private val photoAiMetadataDao: PhotoAiMetadataDao
) : CoroutineWorker(context, params) {
    private val outputFileLock = Any()
    private val processorPool = ConcurrentLinkedQueue<BitmapProcessor>()
    private val createdProcessors = ConcurrentLinkedQueue<BitmapProcessor>()

    private fun acquireProcessor(): BitmapProcessor {
        return processorPool.poll() ?: BitmapProcessor(applicationContext).also {
            createdProcessors.add(it)
        }
    }

    override suspend fun doWork(): Result {
        Timber.tag("WORKER").d("Worker ID = $id")
        val workerStart = System.currentTimeMillis()
        fun logStep(step: String) {
            Timber.tag("PERF")
                .d("[$step] +${System.currentTimeMillis() - workerStart} ms")
        }
        logStep("Worker Started")

        val parallelism = (Runtime.getRuntime().availableProcessors()).coerceIn(12,12 )
        val semaphore = Semaphore(parallelism)
        try {
            setForeground(createForegroundInfo("Menyiapkan pemrosesan..."))
        } catch (e: Exception) {
            Timber.e("Gagal setForeground: ${e.message}")
        }
        logStep("Foreground Ready")
        val accessToken = inputData.getString(KEY_ACCESS_TOKEN).orEmpty()
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
            val uniqueWorkName = "UPLOAD_${hourFolder}_${folder.name}".replace(" ", "_")
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    uniqueWorkName,
                    ExistingWorkPolicy.KEEP,
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

            supervisorScope {
                groupedByHour.forEach { (hourFolder, hourFiles) ->
                    if (isStopped) return@supervisorScope
                    val hourOutputFolder = outputBaseFolder.findFile(hourFolder)
                        ?: outputBaseFolder.createDirectory(hourFolder)

                    Timber.tag("EXPORT").d("Memproses Jam $hourFolder: ${hourFiles.size} foto")

                    if (wmShowPart) {
                        // JALUR 1: PART AKTIF -> Bagi per maxPhotos, selesai Part 1 langsung upload Part 1, dst.
                        val partChunks = hourFiles.chunked(maxPhotos.coerceAtLeast(1))
                        partChunks.forEachIndexed { partIdx, partFiles ->
                            if (isStopped) return@supervisorScope

                            val partNum = startFrom + partIdx
                            val partFolderName = "part $partNum"
                            val currentOutputFolder = hourOutputFolder?.findFile(partFolderName)
                                ?: hourOutputFolder?.createDirectory(partFolderName)

                            val existingFiles = HashSet<String>()
                            currentOutputFolder
                                ?.listFiles()
                                ?.forEach { it.name?.let(existingFiles::add) }

                            partFiles.chunked(4).forEach { subChunk ->
                                if (isStopped) return@supervisorScope
                                processSubBatch(
                                    subFiles = subChunk,
                                    existingFiles = existingFiles,
                                    suffix = suffix,
                                    currentOutputFolder = currentOutputFolder,
                                    imageFiles = imageFiles,
                                    completed = completed,
                                    resize = resize,
                                    watermarkConfig = watermarkConfig,
                                    partName = "$hourFolder / $partFolderName",
                                    quality = quality,
                                    wmShowTimestamp = wmShowTimestamp
                                )
                            }

                            // Selesai 1 Part -> langsung picu upload untuk Part ini!
                            currentOutputFolder?.let { enqueueUpload(it, partFiles.size, hourFolder) }
                        }
                    } else {
                        // JALUR 2: PART MATI -> Pemrosesan dibagi per-batch 500 foto di dalam folder jam
                        val existingFiles = HashSet<String>()
                        hourOutputFolder
                            ?.listFiles()
                            ?.forEach { it.name?.let(existingFiles::add) }

                        val batches = hourFiles.chunked(200)
                        var cumulativeCount = 0

                        batches.forEach { batchFiles ->
                            if (isStopped) return@supervisorScope

                            batchFiles.chunked(4).forEach { subChunk ->
                                if (isStopped) return@supervisorScope
                                processSubBatch(
                                    subFiles = subChunk,
                                    existingFiles = existingFiles,
                                    suffix = suffix,
                                    currentOutputFolder = hourOutputFolder,
                                    imageFiles = imageFiles,
                                    completed = completed,
                                    resize = resize,
                                    watermarkConfig = watermarkConfig,
                                    partName = hourFolder,
                                    quality = quality,
                                    wmShowTimestamp = wmShowTimestamp
                                )
                            }

                            cumulativeCount += batchFiles.size
                            // Sesaat setelah 500 foto selesai diekspor di HP, langsung picu upload!
                            hourOutputFolder?.let { enqueueUpload(it, cumulativeCount, hourFolder) }
                        }
                    }
                }
            }
            val totalWorkerDuration = System.currentTimeMillis() - workerStart
            Timber.tag("PERF").i("⏱️ TOTAL BATCH EXPORT & AI SCAN SELESAI: ${imageFiles.size} foto dalam ${totalWorkerDuration / 1000f} detik (${totalWorkerDuration} ms)")
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

    private suspend fun processSubBatch(
        subFiles: List<ScanPhoto>,
        existingFiles: HashSet<String>,
        suffix: String,
        currentOutputFolder: DocumentFile?,
        imageFiles: List<ScanPhoto>,
        completed: AtomicInteger,
        resize: Int,
        watermarkConfig: WatermarkConfig,
        partName: String,
        quality: Int,
        wmShowTimestamp: Boolean
    ) {
        val subBatchStart = System.currentTimeMillis()
        val decodeStart = System.currentTimeMillis()
        val itemsToProcess = coroutineScope {
            subFiles.map { file ->
                async(Dispatchers.IO) {
                    if (isStopped) return@async null

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
                        null
                    } else {
                        val processor = acquireProcessor()
                        val resizedBitmap = try {
                            processor.decodeAndResize(file.uri, ResizeConfig(percentage = resize))
                        } catch (e: Exception) {
                            Timber.e(e, "Gagal decodeAndResize untuk ${file.name}")
                            null
                        }

                        if (resizedBitmap != null) {
                            ProcessItem(file, outputFileName, outputFile, processor, resizedBitmap)
                        } else {
                            processorPool.offer(processor)
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull().toMutableList()
        }
        val decodeMs = System.currentTimeMillis() - decodeStart

        var aiScanMs = 0L
        var dbInsertMs = 0L

        if (itemsToProcess.isNotEmpty()) {
            // 2. Tahap 2: AI Scan Gemini (Kolase 2x2) jika ada API Key aktif
            val activeKey = geminiKeyManager.getNextAvailableKey()
            if (activeKey != null) {
                val aiStart = System.currentTimeMillis()
                val resizedBitmaps = itemsToProcess.map { it.resizedBitmap }
                val aiResults = try {
                    geminiAnalyzer.analyzeBatch(resizedBitmaps)
                } catch (aiEx: Exception) {
                    Timber.tag("AI_SCAN").e(aiEx, "Gagal Gemini AI Scan Batch")
                    emptyList()
                }
                aiScanMs = System.currentTimeMillis() - aiStart

                val dbStart = System.currentTimeMillis()
                for (i in itemsToProcess.indices) {
                    val item = itemsToProcess[i]
                    val aiMeta = if (i < aiResults.size) aiResults[i] else null
                    val exifTimeStr = extractExifTimeString(item.file.uri, item.file.lastModified)

                    try {
                        photoAiMetadataDao.insert(
                            PhotoAiMetadataEntity(
                                fileName = item.outputFileName,
                                bikeModel = aiMeta?.bikeModel,
                                plateNumber = aiMeta?.plateNumber,
                                bikeColor = aiMeta?.bikeColor,
                                jacketColor = aiMeta?.jacketColor,
                                exifTime = exifTimeStr
                            )
                        )
                        Timber.tag("AI_SCAN").d("💾 Saved Gemini Metadata DB [${item.outputFileName}]: Model=${aiMeta?.bikeModel}, Plat=${aiMeta?.plateNumber}, Motor=${aiMeta?.bikeColor}, Jaket=${aiMeta?.jacketColor}")
                    } catch (dbEx: Exception) {
                        Timber.tag("EXPORT_PERF").e(dbEx, "Gagal insert DB metadata ${item.outputFileName}")
                    }
                }
                dbInsertMs = System.currentTimeMillis() - dbStart
            }

            // 3. Tahap 3: Watermark & Simpan ke Disk
            val wmSaveStart = System.currentTimeMillis()
            for (item in itemsToProcess) {
                try {
                    item.outputFile?.uri?.let { outUri ->
                        applicationContext.contentResolver.openOutputStream(outUri)?.use { rawStream ->
                            BufferedOutputStream(rawStream, 65536).use { outStream ->
                                item.processor.applyWatermarkAndSave(
                                    resizedBitmap = item.resizedBitmap,
                                    watermarkConfig = watermarkConfig,
                                    inputUri = item.file.uri,
                                    fileName = item.outputFileName,
                                    partName = partName,
                                    compressionConfig = CompressionConfig(quality = quality),
                                    outputStream = outStream,
                                    showTimestamp = wmShowTimestamp
                                )
                            }
                        }
                    }
                    val done = completed.incrementAndGet()
                    if (done % 5 == 0 || done == imageFiles.size) {
                        updateExportProgress(done, imageFiles.size, item.file.name ?: "")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Gagal watermark & save ${item.outputFileName}")
                } finally {
                    if (!item.resizedBitmap.isRecycled) item.resizedBitmap.recycle()
                    processorPool.offer(item.processor)
                }
            }
            val wmSaveMs = System.currentTimeMillis() - wmSaveStart
            val totalSubBatchMs = System.currentTimeMillis() - subBatchStart

            Timber.tag("EXPORT_PERF").i(
                "⏱️ [SubBatch ${itemsToProcess.size} Foto] Total=${totalSubBatchMs}ms | " +
                "1.Decode=${decodeMs}ms | 2.AIScan=${aiScanMs}ms | 3.DBInsert=${dbInsertMs}ms | 4.WatermarkSave=${wmSaveMs}ms"
            )
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

    private fun extractExifTimeString(uri: Uri, lastModified: Long): String {
        try {
            applicationContext.contentResolver.openInputStream(uri)?.use { input ->
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
                    if (timePart.contains(':')) {
                        return timePart.trim()
                    }
                }
            }
        } catch (_: Exception) {}

        val cal = Calendar.getInstance()
        if (lastModified > 0L) cal.timeInMillis = lastModified
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(cal.time)
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