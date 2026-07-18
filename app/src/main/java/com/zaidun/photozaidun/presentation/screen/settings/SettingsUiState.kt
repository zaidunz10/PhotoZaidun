package com.zaidun.photozaidun.presentation.screen.settings

data class SettingsUiState(

    val mainFolder: String = "Customer",

    val partFolder: String = "part",

    val fileSuffix: String = "",

    val resizePercent: String = "100",

    val maxPhotoPerFolder: String = "200",

    val autoUpload: Boolean = false

)