package com.zaidun.photozaidun.presentation.screen.settings

import com.zaidun.photozaidun.data.auth.GoogleUser

data class SettingsUiState(
    val googleUser: GoogleUser? = null,
    val loginLoading: Boolean = false,

    val mainFolder: String = "Customer",

    val partFolder: String = "part",

    val fileSuffix: String = "",

    val resizePercent: String = "100",

    val maxPhotoPerFolder: String = "200",

    val autoUpload: Boolean = false

)