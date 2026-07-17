package com.zaidun.photozaidun.data.processor.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

class WatermarkDrawer {

    fun draw(

        bitmap: Bitmap,

        watermark: Bitmap,

        alpha: Float,

        scale: Float,

        position: String

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

        val margin = 24

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

        return result

    }

}