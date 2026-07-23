package com.zaidun.photozaidun.presentation.screen.watermarksettings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class WatermarkSettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<WatermarkSettingsUiState> = combine(
        preferences.watermarkUri,    // 0: String
        preferences.previewImageUri, // 1: String
        preferences.watermarkOpacity, // 2: Float
        preferences.watermarkScale,   // 3: Float
        preferences.showFilename,     // 4: Boolean
        preferences.showPart,         // 5: Boolean
        preferences.showTimestamp,    // 6: Boolean (TAMBAHKAN DI SINI)
        preferences.logoOffsetX,      // 7: Float
        preferences.logoOffsetY,      // 8: Float
        preferences.infoOffsetX,      // 9: Float
        preferences.infoOffsetY,      // 10: Float
        preferences.infoFontSize      // 11: Float
    ) { args ->
        WatermarkSettingsUiState(
            watermarkUri = args[0] as String,
            previewUri = args[1] as String,
            opacity = args[2] as Float,
            scale = args[3] as Float,
            showFilename = args[4] as Boolean,
            showPart = args[5] as Boolean,
            showTimestamp = args[6] as Boolean, // Sesuai index flow ke-6
            logoOffsetX = args[7] as Float,
            logoOffsetY = args[8] as Float,
            infoOffsetX = args[9] as Float,
            infoOffsetY = args[10] as Float,
            infoFontSize = args[11] as Float
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        WatermarkSettingsUiState()
    )

    fun updateLogoSettings(opacity: Float, scale: Float, x: Float, y: Float) {
        viewModelScope.launch {
            preferences.saveWatermarkOpacity(opacity)
            preferences.saveWatermarkScale(scale)
            preferences.saveLogoOffset(x, y)
        }
    }


            fun updateInfoSettings(showFile: Boolean, showPart: Boolean, x: Float, y: Float, size: Float) {
        viewModelScope.launch {
            Timber.d("UPDATE INFO")
            preferences.saveShowFilename(showFile)
            preferences.saveShowPart(showPart)
            preferences.saveInfoOffset(x, y)
            preferences.saveInfoFontSize(size)
        }
    }

    fun updateTimestamp(enabled: Boolean) {
        viewModelScope.launch {
            preferences.saveShowTimestamp(enabled)
        }
    }

    fun savePreviewUri(uri: String) {
        viewModelScope.launch { preferences.savePreviewImageUri(uri) }
    }

    fun saveLogoUri(uri: String) {
        viewModelScope.launch { preferences.saveWatermarkUri(uri) }
    }
}