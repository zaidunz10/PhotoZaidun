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
import javax.inject.Inject

@HiltViewModel
class WatermarkSettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {


    val uiState: StateFlow<WatermarkSettingsUiState> =
        combine(
            preferences.watermarkUri,
            preferences.previewImageUri,
            preferences.watermarkOpacity,
            preferences.watermarkScale,
            preferences.watermarkPosition
        ) { uri, preview, opacity, scale, position ->

            WatermarkSettingsUiState(
                watermarkUri = uri,
                previewUri = preview,
                opacity = opacity,
                scale = scale,
                position = position
            )

        }.combine(preferences.showFilename) { state, showFilename ->

            state.copy(
                showFilename = showFilename
            )

        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            WatermarkSettingsUiState()
        )


    fun save(

        logoUri: String,

        previewUri: String,

        opacity: Float,

        scale: Float,

        position: String,
        showFilename: Boolean

    ) {
        viewModelScope.launch {
            android.util.Log.d("WM_SAVE", "Save dipanggil")
            preferences.saveWatermarkUri(logoUri)

            preferences.savePreviewImageUri(previewUri)

            preferences.saveWatermarkOpacity(opacity)

            preferences.saveWatermarkScale(scale)

            preferences.saveWatermarkPosition(position)
            preferences.saveShowFilename(showFilename)
            android.util.Log.d("WM_SAVE", "Selesai simpan")
        }
    }

    fun saveWithText(
        uri: String,
        text: String,
        opacity: Float,
        scale: Float,
        position: String
    ) {
        viewModelScope.launch {
            preferences.saveWatermarkUri(uri)
            preferences.saveWatermarkText(text)
            preferences.saveWatermarkOpacity(opacity)
            preferences.saveWatermarkScale(scale)
            preferences.saveWatermarkPosition(position)
        }
    }
}
