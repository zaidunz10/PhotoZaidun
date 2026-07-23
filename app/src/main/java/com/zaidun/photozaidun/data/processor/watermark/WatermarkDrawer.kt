package com.zaidun.photozaidun.data.processor.watermark

import android.graphics.*
import com.zaidun.photozaidun.domain.model.TextPosition
import androidx.core.graphics.scale

class WatermarkDrawer {
    private var cachedScaledLogo: Bitmap? = null
    private var cachedWidth = -1
    private var cachedHeight = -1

    fun createPreview(bitmap: Bitmap, maxSize: Int = 720): Bitmap {
        val ratio = bitmap.width.toFloat() / bitmap.height
        return if (bitmap.width >= bitmap.height) {
            bitmap.scale(maxSize, (maxSize / ratio).toInt())
        } else {
            bitmap.scale((maxSize * ratio).toInt(), maxSize)
        }
    }
    fun clearCache() {
        cachedScaledLogo?.recycle()
        cachedScaledLogo = null
        cachedWidth = -1
        cachedHeight = -1
    }

    fun draw(
        bitmap: Bitmap,
        watermark: Bitmap?,
        alpha: Float,
        scale: Float,
        logoOffsetX: Float = 0.02f,
        logoOffsetY: Float = 0.02f,
        showFilename: Boolean = false,
        showPart: Boolean = false,
        fileName: String = "",
        partName: String = "",
        infoOffsetX: Float = 0.02f,
        exifDate: String = "",
        infoOffsetY: Float = 0.05f,
        infoSize: Float = 0.04f,
        textGap: Float = 12f
    ): Bitmap {

        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            isAntiAlias = true
            this.alpha = (alpha * 255).toInt()
        }

        // 1. Draw Logo
        watermark?.let { logo ->
            val targetWidth = (bitmap.width * scale).toInt()
            val targetHeight = (logo.height.toFloat() / logo.width * targetWidth).toInt()
            val resizedLogo =
                if (
                    cachedScaledLogo != null &&
                    cachedWidth == targetWidth &&
                    cachedHeight == targetHeight &&
                    !cachedScaledLogo!!.isRecycled
                ) {
                    cachedScaledLogo!!
                } else {

                    cachedScaledLogo?.recycle()

                    logo.scale(targetWidth, targetHeight).also {

                        cachedScaledLogo = it
                        cachedWidth = targetWidth
                        cachedHeight = targetHeight

                    }
                }
            // Posisi berdasarkan persentase (0.0 - 1.0)
            val x = (bitmap.width - targetWidth) * logoOffsetX
            val y = (bitmap.height - targetHeight) * logoOffsetY

            canvas.drawBitmap(resizedLogo, x, y, paint)
        }

        // 2. Draw Text Block (Nama File & Part)
        val text = buildString {
            if (showFilename) append(fileName.substringBeforeLast('.'))
            if (showFilename && (showPart || exifDate.isNotBlank())) append("  •  ")
            if (showPart) append(partName)
            if (showPart && exifDate.isNotBlank()) append("  •  ")
            if (exifDate.isNotBlank()) append(exifDate)
        }

        if (text.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                this.alpha = (alpha * 255).toInt()
                typeface = Typeface.DEFAULT_BOLD
                this.textSize = bitmap.width * infoSize
                setShadowLayer(8f, 2f, 2f, Color.BLACK)
            }

            val textWidth = textPaint.measureText(text)
            // Posisi teks berdasarkan persentase
            val tx = (bitmap.width - textWidth) * infoOffsetX
            val ty = (bitmap.height - textPaint.textSize) * infoOffsetY + textPaint.textSize

            canvas.drawText(text, tx, ty, textPaint)
        }

        return bitmap
    }
}