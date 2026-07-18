package com.zaidun.photozaidun.presentation.screen.home

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.lifecycle.viewModelScope
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.worker.BatchProcessWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Intent
import android.content.IntentSender
import com.zaidun.photozaidun.data.auth.GoogleAuthManager
import com.zaidun.photozaidun.domain.model.PhotoItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
data class DriveAuthResult(
    val accessToken: String
)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val preferences: UserPreferencesDataStore,
    private val googleAuthManager: GoogleAuthManager,



) : ViewModel() {


    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    init {
        observeUploadProgress()
    }

    private fun observeUploadProgress() {
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("UPLOAD_DRIVE").collect { workInfos ->
                val activeWork = workInfos.filter { !it.state.isFinished }
                val finishedWork = workInfos.filter { it.state == androidx.work.WorkInfo.State.SUCCEEDED }

                _uiState.update { state ->
                    state.copy(
                        isProcessing = activeWork.isNotEmpty(),
                        processedImages = finishedWork.size // Ini contoh sederhana
                    )
                }
            }
        }
    }
    fun requestDrivePermission(
        activity: Activity,
        onNeedUserConsent: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {

        googleAuthManager.requestDriveAccess(

            activity,

            onTokenReady = { token ->

                viewModelScope.launch {

                    preferences.saveDriveAccessToken(token)

                    onEvent(HomeEvent.StartProcess)

                }

            },

            onNeedConsent = onNeedUserConsent,

            onError = onError

        )

    }
    fun onAccessTokenReceived(token: String?) {

        if (token.isNullOrBlank()) {
            Timber.e("Token NULL")
            return
        }

        Timber.d("SAVE TOKEN = $token")

        viewModelScope.launch {

            preferences.saveDriveAccessToken(token)

            Timber.d("TOKEN BERHASIL DISIMPAN")
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.StartProcess -> {
                val inputUri = _uiState.value.selectedFolder
                if (inputUri.isNotEmpty()) {
                    // Membuat request untuk BatchProcessWorker
                    val request = OneTimeWorkRequestBuilder<BatchProcessWorker>()
                        .setInputData(workDataOf("input_folder" to inputUri))
                        .build()

                    // Menjalankan proses di background
                    workManager.enqueue(request)
                }
            }

            HomeEvent.SelectFolder -> {
                // Logika pemilihan folder biasanya ditangani via SAF di UI
            }

            HomeEvent.OpenHistory -> { /* Navigasi ditangani di UI */
            }

            HomeEvent.OpenSettings -> { /* Navigasi ditangani di UI */
            }

            HomeEvent.OpenPreset -> { /* Navigasi ditangani di UI */
            }

            HomeEvent.OpenWatermarkSettings -> { /* Navigasi ditangani di UI */
            }

            HomeEvent.OpenResizeSettings -> { /* Navigasi ditangani di UI */
            }

            is HomeEvent.ToggleDrive -> {
                _uiState.value = _uiState.value.copy(uploadToDrive = event.enabled)
            }

            is HomeEvent.ToggleDarkMode -> {
                _uiState.value = _uiState.value.copy(darkMode = event.enabled)
            }
        }
    }

    // Fungsi tambahan untuk memperbarui state folder dari UI
    fun updateSelectedFolder(
        uri: String,
        count: Int = 0,
        previewPhoto: PhotoItem? = null
    ) {

        _uiState.value = _uiState.value.copy(

            selectedFolder = uri,

            totalImages = count,

            previewPhoto = previewPhoto

        )

        viewModelScope.launch {

            preferences.saveLastFolder(uri)

        }

    }

    fun openGoogleDrive(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://drive.google.com")
            // Memaksa buka aplikasi Drive jika ada
            setPackage("com.google.android.apps.docs")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Jika app Drive tidak ada, buka via Browser
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com")))
        }
    }
    fun startBatchProcess() {
        onEvent(HomeEvent.StartProcess)
    }
}