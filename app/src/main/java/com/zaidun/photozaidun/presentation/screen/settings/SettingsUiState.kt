package com.zaidun.photozaidun.presentation.screen.settings

import com.zaidun.photozaidun.data.auth.GoogleUser
import com.zaidun.photozaidun.domain.model.DriveStorageInfo

data class SettingsUiState(
    val googleUser: GoogleUser? = null,
    val loginLoading: Boolean = false,
    val driveConnected: Boolean = false,
    val photographerWa: String = "",
    val photographerIg: String = "",
    val bankName: String = "BCA",
    val accountNumber: String = "",
    val accountHolderName: String = "",
    val defaultPhotoPrice: Int = 10000,
    val minQtyDiscount: Int = 3,
    val priceDiscount: Int? = null,

    val rootFolder: String = "Photo Zaidun",
    val partFolder: String = "part",
    val folderLevels: List<FolderLevel> = emptyList(),
    val fileSuffix: String = "",
    val startPartNumber: Int = 1,

    val resizePercent: String = "100",

    val maxPhotoPerFolder: String = "200",

    val showFilename: Boolean = false,
    val autoUpload: Boolean = false

)
data class FolderLevel(
    val id: Long = System.currentTimeMillis(),
    val name: String = ""
)
