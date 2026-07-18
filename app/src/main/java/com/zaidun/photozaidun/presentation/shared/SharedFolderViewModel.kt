package com.zaidun.photozaidun.presentation.shared

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.zaidun.photozaidun.domain.model.PhotoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SharedFolderState(
    val folderUri: Uri? = null,
    val folderName: String = "",
    val totalImages: Int = 0
)

@HiltViewModel
class SharedFolderViewModel @Inject constructor() : ViewModel() {
    private val _photos =
        MutableStateFlow<List<PhotoItem>>(emptyList())
    val photos = _photos.asStateFlow()

    private val _state = MutableStateFlow(SharedFolderState())
    val state: StateFlow<SharedFolderState> = _state.asStateFlow()
    private val _folderUri =
        MutableStateFlow<Uri?>(null)
    val folderUri = _folderUri.asStateFlow()

    fun updateFolder(
        uri: Uri,
        folderName: String,
        totalImages: Int,
        photos: List<PhotoItem>
    ) {
        _photos.value = photos

        _state.value = SharedFolderState(
            folderUri = uri,
            folderName = folderName,
            totalImages = totalImages
        )
    }
    fun clear() {
        _photos.value = emptyList()
        _state.value = SharedFolderState()
    }
}