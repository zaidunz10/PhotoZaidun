package com.zaidun.photozaidun.presentation.screen.watermarksettings

data class WatermarkSettingsUiState(
    val watermarkUri: String = "",
    val previewUri: String = "",
    val opacity: Float = 0.8f,
    val scale: Float = 0.2f,
    val showFilename: Boolean = true,
    val showPart: Boolean = true,
    val showTimestamp: Boolean = true,
    val logoOffsetX: Float = 0.02f,
    val logoOffsetY: Float = 0.02f,
    val infoOffsetX: Float = 0.02f,
    val infoOffsetY: Float = 0.05f,
    val infoFontSize: Float = 0.04f,
    val position: String = "" // Untuk kompatibilitas lama jika perlu
)