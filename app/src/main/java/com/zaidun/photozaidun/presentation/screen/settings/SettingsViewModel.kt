package com.zaidun.photozaidun.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {

        viewModelScope.launch {

            preferences.mainFolder.collect {

                _uiState.value =
                    _uiState.value.copy(
                        mainFolder = it
                    )

            }

        }

        viewModelScope.launch {

            preferences.partFolder.collect {

                _uiState.value =
                    _uiState.value.copy(
                        partFolder = it
                    )

            }

        }

        viewModelScope.launch {

            preferences.fileSuffix.collect {

                _uiState.value =
                    _uiState.value.copy(
                        fileSuffix = it
                    )

            }

        }

        viewModelScope.launch {

            preferences.resizePercent.collect {

                _uiState.value =
                    _uiState.value.copy(
                        resizePercent = it.toString()
                    )

            }

        }

        viewModelScope.launch {

            preferences.maxPhotoPerFolder.collect {

                _uiState.value =
                    _uiState.value.copy(
                        maxPhotoPerFolder = it.toString()
                    )

            }

        }

        viewModelScope.launch {

            preferences.autoUpload.collect {

                _uiState.value =
                    _uiState.value.copy(
                        autoUpload = it
                    )

            }

        }

    }

    fun saveMainFolder(value: String) {

        _uiState.value =
            _uiState.value.copy(
                mainFolder = value
            )

        viewModelScope.launch {

            preferences.saveMainFolder(value)

        }

    }

    fun savePartFolder(value: String) {

        _uiState.value =
            _uiState.value.copy(
                partFolder = value
            )

        viewModelScope.launch {

            preferences.savePartFolder(value)

        }

    }

    fun saveFileSuffix(value: String) {

        _uiState.value =
            _uiState.value.copy(
                fileSuffix = value
            )

        viewModelScope.launch {

            preferences.saveFileSuffix(value)

        }

    }

    fun saveResizePercent(value: String) {

        _uiState.value =
            _uiState.value.copy(
                resizePercent = value
            )

        viewModelScope.launch {
            value.toIntOrNull()?.let { preferences.saveResizePercent(it) }

        }

    }

    fun saveMaxPhoto(value: String) {

        _uiState.value =
            _uiState.value.copy(
                maxPhotoPerFolder = value
            )

        viewModelScope.launch {

            value.toIntOrNull()?.let { preferences.saveMaxPhotoPerFolder(it) }

        }

    }

    fun saveAutoUpload(value: Boolean) {

        _uiState.value =
            _uiState.value.copy(
                autoUpload = value
            )

        viewModelScope.launch {

            preferences.saveAutoUpload(value)

        }

    }

}