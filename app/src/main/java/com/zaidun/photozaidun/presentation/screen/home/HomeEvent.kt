package com.zaidun.photozaidun.presentation.screen.home

sealed interface HomeEvent {
    data object SelectFolder : HomeEvent
    data object StartProcess : HomeEvent
    data object OpenHistory : HomeEvent
    data object OpenSettings : HomeEvent
    data object OpenPreset : HomeEvent
    data object OpenWatermarkSettings : HomeEvent
    data object OpenResizeSettings : HomeEvent
    data class ToggleDrive(val enabled: Boolean) : HomeEvent
    data class ToggleDarkMode(val enabled: Boolean) : HomeEvent
}