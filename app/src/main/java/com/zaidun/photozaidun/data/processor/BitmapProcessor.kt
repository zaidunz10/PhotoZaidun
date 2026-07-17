package com.zaidun.photozaidun.data.processor

import android.content.Context
import android.graphics.*
import android.media.ExifInterface
import android.net.Uri
import com.zaidun.photozaidun.domain.model.*
import timber.log.Timber
import java.io.OutputStream

class BitmapProcessor(private val context: Context) {

    fun process(
        inputUri: Uri,
        watermarkConfig: WatermarkConfig,
        resizeConfig: ResizeConfig,
        compressionConfig: CompressionConfig,
        outputStream: OutputStream
    ) {
        var bitmap: Bitmap? = null
        try {
            // 1. Load Bitmap with EXIF orientation
            bitmap = loadBitmapWithOrientation(inputUri) ?: return

            // 2. Resize
            val resizedBitmap = resize(bitmap, resizeConfig)
            if (resizedBitmap != bitmap) {
                bitmap.recycle()
                bitmap = resizedBitmap
            }

            // 3. Watermark
            val watermarkedBitmap = applyWatermark(bitmap, watermarkConfig)
            if (watermarkedBitmap != bitmap) {
                bitmap.recycle()
                bitmap = watermarkedBitmap
            }

            // 4. Save
            val format = when (compressionConfig.format) {
                OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                OutputFormat.WEBP -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
            bitmap.compress(format, compressionConfig.quality, outputStream)
        } catch (e: Exception) {
            Timber.e(e, "Error processing bitmap: ${inputUri}")
            throw e
        } finally {
            bitmap?.recycle()
        }
    }

    private fun loadBitmapWithOrientation(uri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val exifInputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exif = ExifInterface(exifInputStream)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        exifInputStream.close()

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotated
    }

    private fun resize(bitmap: Bitmap, config: ResizeConfig): Bitmap {
        if (config.mode == ResizeMode.PERCENTAGE && config.percentage == 100) {
            return bitmap
        }

        val width = bitmap.width
        val height = bitmap.height

        val factor = when (config.mode) {
            ResizeMode.PERCENTAGE -> config.percentage / 100f
            ResizeMode.LONG_EDGE -> {
                val longEdge = maxOf(width, height)
                if (longEdge > config.maxLongEdge && config.maxLongEdge > 0) {
                    config.maxLongEdge.toFloat() / longEdge
                } else 1f
            }
            ResizeMode.EXACT -> {
                if (config.width > 0 && config.height > 0) {
                    val scaleW = config.width.toFloat() / width
                    val scaleH = config.height.toFloat() / height
                    minOf(scaleW, scaleH)
                } else 1f
            }
        }

        if (factor >= 1f) return bitmap

        val newWidth = (width * factor).toInt()
        val newHeight = (height * factor).toInt()

        return Bitmap.createScaledBitmap(bitmap, maxOf(1, newWidth), maxOf(1, newHeight), true)
    }

    private fun applyWatermark(bitmap: Bitmap, config: WatermarkConfig): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (config.opacity * 255).toInt()
        }

        if (config.type == WatermarkType.IMAGE && config.imageUri != null) {
            val wmBitmap = context.contentResolver.openInputStream(config.imageUri)?.use {
                BitmapFactory.decodeStream(it)
            } ?: return result

            // Scale watermark proporsional (config.size adalah % dari lebar foto)
            val targetWmWidth = (bitmap.width * config.size).toInt()
            val scaleFactor = targetWmWidth.toFloat() / wmBitmap.width
            val targetWmHeight = (wmBitmap.height * scaleFactor).toInt()

            val scaledWm = Bitmap.createScaledBitmap(wmBitmap, targetWmWidth, targetWmHeight, true)
            wmBitmap.recycle()

            val marginX = (bitmap.width * config.marginX)
            val marginY = (bitmap.height * config.marginY)

            val x = when (config.position) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> marginX
                WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> bitmap.width - targetWmWidth - marginX
                WatermarkPosition.CENTER -> (bitmap.width - targetWmWidth) / 2f
            }

            val y = when (config.position) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> marginY
                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> bitmap.height - targetWmHeight - marginY
                WatermarkPosition.CENTER -> (bitmap.height - targetWmHeight) / 2f
            }

            canvas.drawBitmap(scaledWm, x, y, paint)
            scaledWm.recycle()
        } else {
            // Mode Teks
            paint.color = Color.WHITE
            paint.style = Paint.Style.FILL
            // Ukuran font proporsional terhadap lebar foto
            paint.textSize = bitmap.width * config.size * 0.5f

            // Tambahkan Shadow/Outline agar terbaca di background terang
            paint.setShadowLayer(5f, 0f, 0f, Color.BLACK)

            val bounds = Rect()
            paint.getTextBounds(config.text, 0, config.text.length, bounds)

            val marginX = (bitmap.width * config.marginX)
            val marginY = (bitmap.height * config.marginY)

            val x = when (config.position) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> marginX
                WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> bitmap.width - bounds.width() - marginX
                WatermarkPosition.CENTER -> (bitmap.width - bounds.width()) / 2f
            }

            val y = when (config.position) {
                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> marginY + bounds.height()
                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> bitmap.height - marginY
                WatermarkPosition.CENTER -> (bitmap.height + bounds.height()) / 2f
            }

            canvas.drawText(config.text, x, y, paint)
        }
        return result
    }
}