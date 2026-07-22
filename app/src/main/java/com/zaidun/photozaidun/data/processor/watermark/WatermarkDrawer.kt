package com.zaidun.photozaidun.data.processor.watermark

import android.graphics.*
import com.zaidun.photozaidun.domain.model.TextPosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        showDate: Boolean = true,
        showTime: Boolean = true,
        dateText: String = "",
        timeText: String = "",
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
        val line1 = buildString {
            if (showFilename) append(fileName.substringBeforeLast('.'))

            if (showFilename && showPart)
                append("  •  ")

            if (showPart)
                append(partName)
        }

        val now = Date()

        val line2 =
            if (showDate) dateText else ""

        val line3 =
            if (showTime) timeText else ""
        if (
            line1.isNotBlank() ||
            line2.isNotBlank() ||
            line3.isNotBlank()
        ) {

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color = Color.WHITE

                this.alpha = (alpha * 255).toInt()

                typeface = Typeface.DEFAULT_BOLD

                textSize = result.width * infoSize

                setShadowLayer(
                    8f,
                    2f,
                    2f,
                    Color.BLACK
                )
            }

            val widest = listOf(line1, line2, line3)
                .maxOf { textPaint.measureText(it) }

            val startX =
                (result.width - widest) * infoOffsetX

            var currentY =
                (result.height - textPaint.textSize) *
                        infoOffsetY +
                        textPaint.textSize

            val spacing =
                textPaint.textSize + textGap

            if (line1.isNotBlank()) {
                canvas.drawText(
                    line1,
                    startX,
                    currentY,
                    textPaint
                )
                currentY += spacing
            }

            if (line2.isNotBlank()) {
                canvas.drawText(
                    line2,
                    startX,
                    currentY,
                    textPaint
                )
                currentY += spacing
            }

            if (line3.isNotBlank()) {
                canvas.drawText(
                    line3,
                    startX,
                    currentY,
                    textPaint
                )
            }
        }
        return result
        }


    }
