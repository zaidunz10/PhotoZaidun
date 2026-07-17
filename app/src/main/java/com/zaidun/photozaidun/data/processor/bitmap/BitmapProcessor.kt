package com.zaidun.photozaidun.data.processor.bitmap

import android.content.Context
import android.graphics.*
import android.net.Uri
import java.io.OutputStream

// Sediakan enum/data class ini di domain model jika ingin lebih rapi
enum class ResizeMode { PERCENTAGE, EXACT }
enum class OutputFormat { JPEG, PNG, WEBP }

data class WatermarkConfig(
    val text: String = "Photo Zaidun",
    val opacity: Float = 0.8f,
    val size: Float = 0.2f
)

data class ResizeConfig(
    val mode: ResizeMode = ResizeMode.PERCENTAGE,
    val percentage: Int = 80
)

data class CompressionConfig(
    val quality: Int = 85,
    val format: OutputFormat = OutputFormat.JPEG
)

class BitmapProcessor(private val context: Context) {
    fun process(
        inputUri: Uri,
        watermarkConfig: WatermarkConfig,
        resizeConfig: ResizeConfig,
        compressionConfig: CompressionConfig,
        outputStream: OutputStream
    ) {
        val inputStream = context.contentResolver.openInputStream(inputUri)
        var bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close() ?: return

        // 1. Resize
        bitmap = resize(bitmap, resizeConfig)

        // 2. Watermark
        bitmap = applyWatermark(bitmap, watermarkConfig)

        // 3. Simpan
        val format = when (compressionConfig.format) {
            OutputFormat.PNG -> Bitmap.CompressFormat.PNG
            OutputFormat.WEBP -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        bitmap.compress(format, compressionConfig.quality, outputStream)
        bitmap.recycle()
    }

    private fun resize(bitmap: Bitmap, config: ResizeConfig): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val factor = if (config.mode == ResizeMode.PERCENTAGE) config.percentage / 100f else 1f
        val newWidth = (width * factor).toInt()
        val newHeight = (height * factor).toInt()

        return Bitmap.createScaledBitmap(bitmap, maxOf(1, newWidth), maxOf(1, newHeight), true)
    }

    private fun applyWatermark(bitmap: Bitmap, config: WatermarkConfig): Bitmap {

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = (config.opacity * 255).toInt()
            color = Color.WHITE
            textSize = bitmap.width * config.size * 0.5f
        }

        // Gambar teks di posisi kanan bawah (default)
        canvas.drawText(config.text, bitmap.width * 0.1f, bitmap.height * 0.9f, paint)
        return result
    }
}