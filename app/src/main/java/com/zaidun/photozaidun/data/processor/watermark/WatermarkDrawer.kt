package com.zaidun.photozaidun.data.processor.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.Color
import com.zaidun.photozaidun.domain.model.TextPosition

class WatermarkDrawer {

    fun createPreview(
        bitmap: Bitmap,
        maxSize: Int = 720
    ): Bitmap {

        val ratio = bitmap.width.toFloat() / bitmap.height

        return if (bitmap.width >= bitmap.height) {

            Bitmap.createScaledBitmap(
                bitmap,
                maxSize,
                (maxSize / ratio).toInt(),
                true
            )

        } else {

            Bitmap.createScaledBitmap(
                bitmap,
                (maxSize * ratio).toInt(),
                maxSize,
                true
            )

        }
    }
    fun draw(
        bitmap: Bitmap,
        watermark: Bitmap,
        alpha: Float,
        scale: Float,
        position: String,

        showFilename: Boolean = false,
        showPart: Boolean = false,

        fileName: String = "",
        partName: String = "",

        textSize: Float = 0.045f,
        textGap: Float = 12f,

        textPosition: TextPosition = TextPosition.BELOW_LOGO
    ): Bitmap {

        val result =
            bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(result)

        val paint = Paint().apply {

            isAntiAlias = true

            this.alpha = (alpha * 255).toInt()

        }

        val targetWidth =
            (result.width * scale).toInt()

        val targetHeight =
            (watermark.height.toFloat() /
                    watermark.width *
                    targetWidth).toInt()

        val resizedLogo = Bitmap.createScaledBitmap(

            watermark,

            targetWidth,

            targetHeight,

            true

        )

        val margin =
            (result.width * 0.02f).toInt()

        val x: Float
        val y: Float

        when (position) {

            "TOP_LEFT" -> {

                x = margin.toFloat()

                y = margin.toFloat()

            }

            "TOP_RIGHT" -> {

                x =
                    (result.width - targetWidth - margin).toFloat()

                y = margin.toFloat()

            }

            "BOTTOM_LEFT" -> {

                x = margin.toFloat()

                y =
                    (result.height - targetHeight - margin).toFloat()

            }
            "CENTER" -> {

                x = ((result.width - targetWidth) / 2f)

                y = ((result.height - targetHeight) / 2f)

            }

            else -> {

                x =
                    (result.width - targetWidth - margin).toFloat()

                y =
                    (result.height - targetHeight - margin).toFloat()

            }

        }

        canvas.drawBitmap(

            resizedLogo,

            x,

            y,

            paint

        )
        val text = buildString {

            if (showFilename) {

                append(fileName.substringBeforeLast('.'))

            }

            if (showFilename && showPart) {

                append("   |   ")

            }

            if (showPart) {

                append(partName)

            }
        }

        if (text.isNotBlank()) {

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {

                color = Color.WHITE

                this.alpha = (alpha * 255).toInt()

                typeface = Typeface.DEFAULT_BOLD

                this.textSize = bitmap.width * textSize

                setShadowLayer(
                    8f,
                    2f,
                    2f,
                    Color.BLACK
                )
            }

            if (textPosition == TextPosition.BELOW_LOGO) {

                val textWidth = textPaint.measureText(text)

                val tx = when (position) {

                    "TOP_LEFT",
                    "BOTTOM_LEFT" ->
                        x

                    "TOP_RIGHT",
                    "BOTTOM_RIGHT" ->
                        x + targetWidth - textWidth

                    "CENTER" ->
                        x + (targetWidth - textWidth) / 2f

                    else ->
                        x
                }

                val ty =
                    y + targetHeight + textGap + textPaint.textSize

                canvas.drawText(
                    text,
                    tx,
                    ty,
                    textPaint
                )

            } else {

                val tx =
                    x + targetWidth + textGap

                val ty =
                    y + textPaint.textSize

                canvas.drawText(
                    text,
                    tx,
                    ty,
                    textPaint
                )
            }
        }

        return result

    }

}