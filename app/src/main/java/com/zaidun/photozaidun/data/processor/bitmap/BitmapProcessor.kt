package com.zaidun.photozaidun.data.processor.bitmap
import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.zaidun.photozaidun.data.processor.watermark.WatermarkDrawer
import com.zaidun.photozaidun.domain.model.*
import timber.log.Timber
import java.io.OutputStream


class BitmapProcessor(private val context: Context) {
    fun process(
        inputUri: Uri,
        watermarkConfig: WatermarkConfig,
        resizeConfig: ResizeConfig,
        fileName: String,
        partName: String,
        compressionConfig: CompressionConfig,
        outputStream: OutputStream
    ) {


        var bitmap: Bitmap? = null
        try {
            bitmap = loadFixedBitmap(inputUri) ?: return

            // 1. Resize
            val resized = resize(bitmap, resizeConfig)
            if (resized != bitmap) { bitmap.recycle(); bitmap = resized }

            // 2. Watermark (Kirim fileName ke sini)
            val watermarked = applyWatermark(bitmap, fileName,partName, watermarkConfig)
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

    private fun loadFixedBitmap(uri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        val exifStream = context.contentResolver.openInputStream(uri) ?: return original
        val exif = ExifInterface(exifStream)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        exifStream.close()
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return original
        }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        original.recycle()
        return rotated
    }

    private fun resize(bitmap: Bitmap, config: ResizeConfig): Bitmap {
        val factor = config.percentage / 100f
        if (factor >= 1f) return bitmap
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt(), (bitmap.height * factor).toInt(), true)
    }
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

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

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

        return result
    }
    private fun applyImageWatermark(
        bitmap: Bitmap,
        fileName: String,
        partName: String,
        config: WatermarkConfig
    ): Bitmap {

        val uri = config.imageUri ?: return bitmap

        val input = context.contentResolver.openInputStream(uri)
            ?: return bitmap

        val logo = BitmapFactory.decodeStream(input)
        input.close()

        if (logo == null) {
            return bitmap
        }

        val drawer = WatermarkDrawer()

        val result = drawer.draw(
            bitmap = bitmap,
            watermark = logo,
            alpha = config.opacity,
            scale = config.size,
            position = config.position.name,

            showFilename = config.showFilename,
            showPart = true,

            fileName = fileName,
            partName = partName,

            textSize = config.textSize,
            textGap = config.textGap,
            textPosition = config.textPosition
        )
        logo.recycle()

        return result
    }

    private fun applyWatermark(
        bitmap: Bitmap,
        fileName: String,
        partName: String,
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
                    config,

                )
        }
    }
}