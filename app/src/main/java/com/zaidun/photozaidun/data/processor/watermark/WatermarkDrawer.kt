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
        textGap: Float = 12f // Jarak antar baris
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
            val x = (bitmap.width - targetWidth) * logoOffsetX
            val y = (bitmap.height - targetHeight) * logoOffsetY
            canvas.drawBitmap(resizedLogo, x, y, paint)
        }

        // 2. Draw Text Block (Dua Baris)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.alpha = (alpha * 255).toInt()
            typeface = Typeface.DEFAULT_BOLD
            this.textSize = bitmap.width * infoSize
            setShadowLayer(8f, 2f, 2f, Color.BLACK)
        }

        val line1 = buildString {
            if (showFilename) append(fileName.substringBeforeLast('.'))
            if (showFilename && showPart) append("  •  ")
            if (showPart) append(partName)
        }
        val line2 = exifDate

        // Hitung posisi Y baris paling bawah (Anchor)
        var currentY = (bitmap.height - textPaint.textSize) * infoOffsetY + textPaint.textSize

        // Gambar Baris 2 (Tanggal) di paling bawah
        if (line2.isNotBlank()) {
            val textWidth2 = textPaint.measureText(line2)
            val tx2 = (bitmap.width - textWidth2) * infoOffsetX
            canvas.drawText(line2, tx2, currentY, textPaint)

            // Naikkan posisi Y untuk baris di atasnya (Nama File)
            currentY -= (textPaint.textSize + textGap)
        }

        // Gambar Baris 1 (Nama File & Part) di atas tanggal
        if (line1.isNotBlank()) {
            val textWidth1 = textPaint.measureText(line1)
            val tx1 = (bitmap.width - textWidth1) * infoOffsetX
            canvas.drawText(line1, tx1, currentY, textPaint)
        }

        return bitmap
    }
}