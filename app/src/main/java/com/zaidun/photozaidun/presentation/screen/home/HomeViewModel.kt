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
import androidx.work.WorkInfo
import com.zaidun.photozaidun.data.auth.GoogleAuthManager
import com.zaidun.photozaidun.domain.model.PhotoItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.util.UUID
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

    private var currentWorkId: UUID? = null
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private fun observeCurrentWorker() {

        val id = currentWorkId ?: return

        workManager
            .getWorkInfoByIdLiveData(id)
            .observeForever { work ->
        Timber.d("OBSERVER MASUK")

                if (work == null) return@observeForever

                val progress = work.progress

                Timber.d("STATE = ${work.state}")

                _uiState.update {

                    it.copy(

                        isProcessing =
                            work.state == WorkInfo.State.ENQUEUED ||
                                    work.state == WorkInfo.State.RUNNING,

                        progress =
                            progress.getInt("progress", 0).toFloat(),

                        current =
                            progress.getInt("current", 0),

                        total =
                            progress.getInt("total", 0),

                        currentFilename =
                            progress.getString("filename") ?: ""

                    )

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
    fun cancelExport() {

        workManager.cancelAllWorkByTag("EXPORT")

    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.StartProcess -> {
                Timber.tag("EXPORT").d("START PROCESS DIPANGGIL")
                _uiState.update {
                    it.copy(
                        isProcessing = true,
                        progress = 0f,
                        current = 0,
                        total = 0,
                        currentFilename = ""
                    )
                }

                val inputUri = _uiState.value.selectedFolder

                if (inputUri.isNotEmpty()) {

                    val request = OneTimeWorkRequestBuilder<BatchProcessWorker>()
                        .setInputData(
                            workDataOf(
                                "input_folder" to inputUri
                            )
                        )
                        .build()

                    currentWorkId = request.id

                    observeCurrentWorker()

                    workManager.enqueue(request)
                    Timber.tag("EXPORT").d("ENQUEUE = ${request.id}")
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