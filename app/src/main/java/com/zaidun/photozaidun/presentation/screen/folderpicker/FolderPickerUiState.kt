package com.zaidun.photozaidun.presentation.screen.folderpicker

import android.net.Uri
import com.zaidun.photozaidun.domain.model.PhotoItem

data class FolderPickerUiState(

    val selectedFolderUri: Uri? = null,

    val folderName: String = "",

    val photos: List<PhotoItem> = emptyList(),

    val scanned: Int = 0,

    val currentBatch: Int = 0,

    val scanning: Boolean = false,

    val error: String? = null
)