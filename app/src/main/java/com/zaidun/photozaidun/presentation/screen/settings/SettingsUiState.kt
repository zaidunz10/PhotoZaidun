package com.zaidun.photozaidun.presentation.screen.settings

import com.zaidun.photozaidun.data.auth.GoogleUser
import com.zaidun.photozaidun.domain.model.DriveStorageInfo

data class SettingsUiState(
    val googleUser: GoogleUser? = null,
    val loginLoading: Boolean = false,
    val driveConnected: Boolean = false,

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
