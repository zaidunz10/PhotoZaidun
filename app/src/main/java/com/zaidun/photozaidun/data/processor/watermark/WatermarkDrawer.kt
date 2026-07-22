package com.zaidun.photozaidun.data.processor.watermark

import android.graphics.*
import com.zaidun.photozaidun.domain.model.TextPosition

class WatermarkDrawer {

    fun createPreview(bitmap: Bitmap, maxSize: Int = 720): Bitmap {
        val ratio = bitmap.width.toFloat() / bitmap.height
        return if (bitmap.width >= bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, maxSize, (maxSize / ratio).toInt(), true)
        } else {
            Bitmap.createScaledBitmap(bitmap, (maxSize * ratio).toInt(), maxSize, true)
        }
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
        infoOffsetY: Float = 0.05f,
        infoSize: Float = 0.04f,
        textGap: Float = 12f
    ): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            isAntiAlias = true
            this.alpha = (alpha * 255).toInt()
        }

        // 1. Draw Logo
        watermark?.let { logo ->
            val targetWidth = (result.width * scale).toInt()
            val targetHeight = (logo.height.toFloat() / logo.width * targetWidth).toInt()
            val resizedLogo = Bitmap.createScaledBitmap(logo, targetWidth, targetHeight, true)

            // Posisi berdasarkan persentase (0.0 - 1.0)
            val x = (result.width - targetWidth) * logoOffsetX
            val y = (result.height - targetHeight) * logoOffsetY

            canvas.drawBitmap(resizedLogo, x, y, paint)
            resizedLogo.recycle()
        }

        // 2. Draw Text Block (Nama File & Part)
        val text = buildString {
            if (showFilename) append(fileName.substringBeforeLast('.'))
            if (showFilename && showPart) append("  •  ")
            if (showPart) append(partName)
        }

        if (text.isNotBlank()) {
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                this.alpha = (alpha * 255).toInt()
                typeface = Typeface.DEFAULT_BOLD
                this.textSize = result.width * infoSize
                setShadowLayer(8f, 2f, 2f, Color.BLACK)
            }

            val textWidth = textPaint.measureText(text)
            // Posisi teks berdasarkan persentase
            val tx = (result.width - textWidth) * infoOffsetX
            val ty = (result.height - textPaint.textSize) * infoOffsetY + textPaint.textSize

            canvas.drawText(text, tx, ty, textPaint)
        }

        return result
    }
}