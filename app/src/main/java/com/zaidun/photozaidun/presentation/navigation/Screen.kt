package com.zaidun.photozaidun.presentation.navigation

sealed class Screen(val route: String) {

    data object Splash : Screen("splash")

    data object Home : Screen("home")

    data object FolderPicker : Screen("folder_picker")

    data object Watermark : Screen("watermark")

    data object Resize : Screen("resize")
    object ExportLoading : Screen("export_loading")

    data object Compression : Screen("compression")

    data object Export : Screen("export")

    data object Progress : Screen("progress")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Contact : Screen("contact")
    data object Drive : Screen("drive")
    data object PhotoPreview : Screen("photo_preview")
}