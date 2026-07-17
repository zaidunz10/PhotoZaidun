package com.zaidun.photozaidun.presentation.screen.folderpicker

import android.net.Uri

sealed interface FolderPickerEvent {

    data object PickFolder : FolderPickerEvent

    data class FolderSelected(
        val uri: Uri
    ) : FolderPickerEvent

    data object Refresh : FolderPickerEvent
}