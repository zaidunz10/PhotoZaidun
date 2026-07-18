package com.zaidun.photozaidun.presentation.screen.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.startup.StartupLogger
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.auth.GoogleAuthManager
import com.zaidun.photozaidun.data.auth.GoogleUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber.Forest.e
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore,
    private val googleAuthManager: GoogleAuthManager

) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.userId,
                preferences.userName,
                preferences.userEmail
            ) { id, name, email ->
                if (id.isNotEmpty()) GoogleUser(id, name, email, null) else null
            }.collect { user ->
                _uiState.update { it.copy(googleUser = user) }
            }
        }
    }
    init{

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
    fun loginGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(loginLoading = true) }
            try {
                val user = googleAuthManager.signIn(context)
                if (user != null) {
                    // INI YANG PENTING:
                    preferences.saveUser(user)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(loginLoading = false) }
                // Log error atau tampilkan Toast agar tidak crash
                android.util.Log.e("SettingsViewModel", "Login gagal: ${e.message}")
            }
        }
    }
    fun logoutGoogle(
        activity: Activity
    ) {

        viewModelScope.launch {

            googleAuthManager.signOut(activity)

            _uiState.update {

                it.copy(
                    googleUser = null
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