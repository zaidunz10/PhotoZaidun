package com.zaidun.photozaidun.presentation.screen.watermarksettings

data class WatermarkSettingsUiState(
    val watermarkUri: String = "",

    val previewUri: String = "",

    val opacity: Float = .8f,

    val scale: Float = .2f,

    val position: String = "BOTTOM_RIGHT"

)