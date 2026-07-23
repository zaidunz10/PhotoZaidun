package com.zaidun.photozaidun.data.processor.bitmap
import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.zaidun.photozaidun.data.processor.watermark.WatermarkDrawer
import com.zaidun.photozaidun.domain.model.*
import timber.log.Timber
import java.io.OutputStream
import androidx.core.graphics.scale
import java.text.SimpleDateFormat
import java.util.*
class BitmapProcessor(private val context: Context) {
    private val drawer = WatermarkDrawer()
    fun clearCache() {
        cachedLogo?.recycle()
        cachedLogo = null
        cachedLogoUri = null

        drawer.clearCache()
    }
    // Di dalam BitmapProcessor.kt

    private fun getOriginalDateTime(uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                // TAG_DATETIME_ORIGINAL biasanya berisi format "yyyy:MM:dd HH:mm:ss"
                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)

                if (!dateTime.isNullOrBlank()) {
                    // Ubah format "2023:10:25 14:30:05" menjadi "25-10-2023 14:30"
                    val parts = dateTime.split(" ")
                    val dateParts = parts[0].split(":")
                    val timeParts = parts[1].split(":")
                    "${dateParts[2]}-${dateParts[1]}-${dateParts[0]}  ${timeParts[0]}:${timeParts[1]}"
                } else {
                    ""
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun process(
        inputUri: Uri,
        watermarkConfig: WatermarkConfig,
        resizeConfig: ResizeConfig,
        fileName: String,
        partName: String,
        compressionConfig: CompressionConfig,
        outputStream: OutputStream,
        showTimestamp: Boolean
    ) {
        val exifDate = if (showTimestamp) getExifDate(inputUri) else ""

        var bitmap: Bitmap? = null
        try {
            bitmap = loadFixedBitmap(
                inputUri,
                resizeConfig
            ) ?: return

            // 1. Resize
            val resized = resize(bitmap, resizeConfig)
            if (resized != bitmap) { bitmap.recycle(); bitmap = resized }

            // 2. Watermark (Kirim fileName ke sini)
            val watermarked = applyWatermark(bitmap, fileName, partName, exifDate, watermarkConfig)
            if (watermarked != bitmap) { bitmap.recycle(); bitmap = watermarked }

            // 3. Simpan
            val format = when (compressionConfig.format) {
                OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                OutputFormat.WEBP -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }

            bitmap.compress(format, compressionConfig.quality, outputStream)
        } catch (e: Exception) {
            Timber.e(e, "Gagal memproses bitmap")
            throw e
        } finally {
            bitmap?.recycle()
        }
    }
    private var cachedLogo: Bitmap? = null
    private var cachedLogoUri: Uri? = null

    private fun getExifDate(uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                // Format asli EXIF: "yyyy:MM:dd HH:mm:ss"
                val dateString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)

                if (!dateString.isNullOrBlank()) {
                    val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
                    val date = parser.parse(dateString)

                    if (date != null) {
                        // Format: "Kamis, 25 Oktober 2023  14:30"
                        val formatter = SimpleDateFormat("EEEE, d MMMM yyyy  HH:mm", Locale("id", "ID"))
                        formatter.format(date)
                    } else {
                        ""
                    }
                } else {
                    ""
                }
            } ?: ""
        } catch (e: Exception) {
            Timber.e(e, "Gagal membaca EXIF date")
            ""
        }
    }
    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        resizeConfig: ResizeConfig
    ): Int {

        val factor = resizeConfig.percentage / 100f

        if (factor >= 1f)
            return 1

        val targetWidth = (width * factor).toInt()
        val targetHeight = (height * factor).toInt()

        var sample = 1

        while (
            width / sample > targetWidth * 2 ||
            height / sample > targetHeight * 2
        ) {
            sample *= 2
        }

        return sample
    }
    private fun loadFixedBitmap(
        uri: Uri,
        resizeConfig: ResizeConfig
    ): Bitmap? {

        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        val sampleSize = calculateInSampleSize(
            bounds.outWidth,
            bounds.outHeight,
            resizeConfig
        )

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }

        val original = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null
        val mutableBitmap =
            if (original.isMutable)
                original
            else
                original.copy(Bitmap.Config.ARGB_8888, true).also {
                    original.recycle()
                }

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 ->
                matrix.postRotate(90f)

            ExifInterface.ORIENTATION_ROTATE_180 ->
                matrix.postRotate(180f)

            ExifInterface.ORIENTATION_ROTATE_270 ->
                matrix.postRotate(270f)

            else ->
                return mutableBitmap
        }

        val rotated = Bitmap.createBitmap(
            mutableBitmap,
            0,
            0,
            mutableBitmap.width,
            mutableBitmap.height,
            matrix,
            true
        )

        if (rotated != mutableBitmap) {
            mutableBitmap.recycle()
        }

        return rotated
    }

    private fun resize(bitmap: Bitmap, config: ResizeConfig): Bitmap {
        val factor = config.percentage / 100f
        if (factor >= 1f) return bitmap
        val width = maxOf(1, (bitmap.width * factor).toInt())
        val height = maxOf(1, (bitmap.height * factor).toInt())

        if (width == bitmap.width && height == bitmap.height) {
            return bitmap
        }

        return bitmap.scale(width, height)  }
    private fun applyTextWatermark(
        bitmap: Bitmap,
        fileName: String,
        config: WatermarkConfig
    ): Bitmap {

        val fullText = buildString {
            append(config.text)

            if (config.showFilename) {
                append("\n")
                append(fileName.substringBeforeLast('.'))
            }
        }

        val canvas = Canvas(bitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (config.opacity * 255).toInt()
            typeface = Typeface.DEFAULT_BOLD
            color = Color.WHITE
            textSize = bitmap.width * config.size * 0.4f

            setShadowLayer(
                8f,
                2f,
                2f,
                Color.BLACK
            )
        }

        val lines = fullText.split("\n")

        var y = bitmap.height * 0.95f

        for (i in lines.indices.reversed()) {
            canvas.drawText(
                lines[i],
                bitmap.width * 0.05f,
                y,
                paint
            )
            y -= paint.textSize + 10f
        }

        return bitmap
    }
    private fun applyImageWatermark(
        bitmap: Bitmap,
        fileName: String,
        partName: String,
        exifDate: String,
        config: WatermarkConfig
    ): Bitmap {

        val uri = config.imageUri ?: return bitmap

        val logo = if (
            cachedLogo != null &&
            cachedLogoUri == uri &&
            !cachedLogo!!.isRecycled
        ) {
            cachedLogo!!
        } else {

            cachedLogo?.recycle()

            context.contentResolver.openInputStream(uri)?.use {

                BitmapFactory.decodeStream(
                    it,
                    null,
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                )

            }?.also {

                cachedLogo = it
                cachedLogoUri = uri

            } ?: return bitmap
        }


        val result = drawer.draw(
            bitmap = bitmap,
            watermark = logo,
            alpha = config.opacity,
            scale = config.size,
            logoOffsetX = config.logoOffsetX,
            logoOffsetY = config.logoOffsetY,
            exifDate = exifDate,
            showFilename = config.showFilename,
            showPart = config.showPart,

            fileName = fileName,
            partName = partName,

            infoOffsetX = config.infoOffsetX,
            infoOffsetY = config.infoOffsetY,
            infoSize = config.infoSize
        )


        return result
    }

    private fun applyWatermark(
        bitmap: Bitmap,
        fileName: String,
        partName: String,
        exifDate: String,
        config: WatermarkConfig

    ): Bitmap {

        return when (config.type) {

            WatermarkType.TEXT ->
                applyTextWatermark(
                    bitmap,
                    fileName,
                    config
                )

            WatermarkType.IMAGE ->
                applyImageWatermark(
                    bitmap,
                    fileName,
                    partName,
                    exifDate,
                    config

                )
        }
    }
}