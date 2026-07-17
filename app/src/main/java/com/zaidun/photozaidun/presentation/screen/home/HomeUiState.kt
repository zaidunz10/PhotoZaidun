package com.zaidun.photozaidun.presentation.screen.home

data class HomeUiState(

    val selectedFolder: String = "",

    val totalImages: Int = 0,

    val totalSelected: Int = 0,

    val isProcessing: Boolean = false,

    val progress: Float = 0f,

    val processedImages: Int = 0,

    val failedImages: Int = 0,

    val uploadToDrive: Boolean = false,

    val darkMode: Boolean = false
)