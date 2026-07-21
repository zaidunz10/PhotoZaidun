package com.zaidun.photozaidun.presentation.screen.home
import com.zaidun.photozaidun.domain.model.PhotoItem

data class HomeUiState(

    val selectedFolder: String = "",

    val totalImages: Int = 0,

    val totalSelected: Int = 0,

    val isProcessing: Boolean = false,
    val progress: Float = 0f,

    val previewPhoto: PhotoItem? = null,

    val processedImages: Int = 0,

    val failedImages: Int = 0,

    val uploadToDrive: Boolean = false,

    val darkMode: Boolean = false,
    val currentFilename: String = "",
    val current: Int = 0,
    val total: Int = 0
)