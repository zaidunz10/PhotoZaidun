package com.zaidun.photozaidun.domain.model

import android.net.Uri

enum class WatermarkType { TEXT, IMAGE }
enum class WatermarkPosition { TOP_LEFT, TOP_RIGHT, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT }
enum class ResizeMode { PERCENTAGE, EXACT, LONG_EDGE }
enum class OutputFormat { JPEG, PNG, WEBP }

data class WatermarkConfig(
    val type: WatermarkType = WatermarkType.TEXT,
    val text: String = "Photo Zaidun",
    val imageUri: Uri? = null,
    val opacity: Float = 0.8f,
    val size: Float = 0.2f,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val marginX: Float = 0.05f,
    val marginY: Float = 0.05f,
    val showFilename: Boolean = true,
    val showPart: Boolean = true,
    val textSize: Float = 0.045f,
    val textPosition: TextPosition = TextPosition.BELOW_LOGO,
    val textGap: Float = 12f,
    val logoOffsetX: Float = 0.02f,
    val logoOffsetY: Float = 0.02f,
    val infoOffsetX: Float = 0.02f,
    val infoOffsetY: Float = 0.05f,
    val infoSize: Float = 0.04f
)

data class ResizeConfig(
    val mode: ResizeMode = ResizeMode.PERCENTAGE,
    val percentage: Int = 80,
    val width: Int = 0,
    val height: Int = 0,
    val maxLongEdge: Int = 0
)

data class CompressionConfig(
    val quality: Int = 85,
    val format: OutputFormat = OutputFormat.JPEG
)