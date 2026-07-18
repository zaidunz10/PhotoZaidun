package com.zaidun.photozaidun.presentation.screen.settings

sealed interface SettingsEvent {

    data class MainFolderChanged(
        val value: String
    ) : SettingsEvent

    data class PartFolderChanged(
        val value: String
    ) : SettingsEvent

    data class FileSuffixChanged(
        val value: String
    ) : SettingsEvent

    data class ResizeChanged(
        val value: String
    ) : SettingsEvent

    data class MaxPhotoChanged(
        val value: String
    ) : SettingsEvent

    data class AutoUploadChanged(
        val value: Boolean
    ) : SettingsEvent
}