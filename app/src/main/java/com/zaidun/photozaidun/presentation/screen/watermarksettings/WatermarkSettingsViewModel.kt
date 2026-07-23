package com.zaidun.photozaidun.presentation.screen.watermarksettings

import android.content.Context
import android.net.Uri
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
        preferences.watermarkUri,
        preferences.previewImageUri,
        preferences.watermarkOpacity,
        preferences.watermarkScale,
        preferences.showFilename,
        preferences.showPart,
        preferences.logoOffsetX,
        preferences.logoOffsetY,
        preferences.infoOffsetX,
        preferences.infoOffsetY,
        preferences.infoFontSize
    ) { args ->
        WatermarkSettingsUiState(
            watermarkUri = args[0] as String,
            previewUri = args[1] as String,
            opacity = args[2] as Float,
            scale = args[3] as Float,
            showFilename = args[4] as Boolean,
            showPart = args[5] as Boolean,
            logoOffsetX = args[6] as Float,
            logoOffsetY = args[7] as Float,
            infoOffsetX = args[8] as Float,
            infoOffsetY = args[9] as Float,
            infoFontSize = args[10] as Float
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

    fun savePreviewUri(uri: String) {
        viewModelScope.launch { preferences.savePreviewImageUri(uri) }
    }

    fun saveLogoUri(uri: String) {
        viewModelScope.launch { preferences.saveWatermarkUri(uri) }
    }
}