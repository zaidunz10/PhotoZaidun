package com.zaidun.photozaidun.presentation.screen.settings

import android.app.Activity
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.auth.GoogleAuthManager
import com.zaidun.photozaidun.data.auth.GoogleUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore,
    private val googleAuthManager: GoogleAuthManager
) : ViewModel() {
    fun saveAutoUpload(value: Boolean) {
        _uiState.update {
            it.copy(autoUpload = value)
        }

        viewModelScope.launch {
            preferences.saveAutoUpload(value)
        }
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // PERBAIKAN: Menggunakan Array<Any?> karena flow yang digabung > 5
        viewModelScope.launch {
            combine(
                preferences.userId,
                preferences.userName,
                preferences.userEmail,
                preferences.rootFolder,
                preferences.mainFolder,
                preferences.partFolder,
                preferences.fileSuffix,
                preferences.resizePercent,
                preferences.maxPhotoPerFolder,
                preferences.autoUpload,
                preferences.driveAccessToken

            ) { args: Array<Any?> ->
                val id = args[0] as String
                val name = args[1] as String
                val email = args[2] as String
                val root = args[3] as String
                val main = args[4] as String
                val part = args[5] as String
                val suffix = args[6] as String
                val resize = args[7] as Int
                val max = args[8] as Int
                val auto = args[9] as Boolean
                val token = args[10] as String

                val user = if (id.isNotEmpty()) GoogleUser(id, name, email, null) else null

                _uiState.update {
                    it.copy(
                        googleUser = user,
                        driveConnected = token.isNotBlank(),
                        rootFolder = root,
                        mainFolder = main,
                        partFolder = part,
                        fileSuffix = suffix,
                        resizePercent = resize.toString(),
                        maxPhotoPerFolder = max.toString(),
                        autoUpload = auto
                    )
                }
            }.collect()
        }
    }

    fun loginGoogle(
        activity: Activity,
        onNeedConsent: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // 1. Login Identitas
                val user = googleAuthManager.signIn(activity)
                if (user != null) {
                    preferences.saveUser(user)

                    // 2. LANGSUNG MINTA IZIN DRIVE (Scope: DRIVE_FILE)
                    googleAuthManager.requestDriveAccess(
                        activity = activity,
                        onTokenReady = { token ->
                            viewModelScope.launch { preferences.saveDriveAccessToken(token) }

                                Timber.tag("Drive").d("TOKEN DITERIMA = $token")

                        },
                        onNeedConsent = onNeedConsent,
                        onError = onError
                    )
                }
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun logoutGoogle(activity: Activity) {
        viewModelScope.launch {
            googleAuthManager.signOut(activity)
            preferences.clearUser()
            preferences.saveDriveAccessToken("")
            _uiState.update { it.copy(googleUser = null) }
        }
    }


    // Fungsi Save Responsif (Update UI Langsung + Simpan DB)
    fun saveRootFolder(v: String) { _uiState.update { it.copy(rootFolder = v) }; viewModelScope.launch { preferences.saveRootFolder(v) } }
    fun saveMainFolder(v: String) { _uiState.update { it.copy(mainFolder = v) }; viewModelScope.launch { preferences.saveMainFolder(v) } }
    fun savePartFolder(v: String) { _uiState.update { it.copy(partFolder = v) }; viewModelScope.launch { preferences.savePartFolder(v) } }
    fun saveFileSuffix(v: String) { _uiState.update { it.copy(fileSuffix = v) }; viewModelScope.launch { preferences.saveFileSuffix(v) } }

    fun saveResizePercent(v: String) {
        _uiState.update { it.copy(resizePercent = v) }
        viewModelScope.launch { v.toIntOrNull()?.let { preferences.saveResizePercent(it) } }
    }
    fun saveMaxPhoto(v: String) {
        _uiState.update { it.copy(maxPhotoPerFolder = v) }
        viewModelScope.launch { v.toIntOrNull()?.let { preferences.saveMaxPhotoPerFolder(it) } }
    }
}
