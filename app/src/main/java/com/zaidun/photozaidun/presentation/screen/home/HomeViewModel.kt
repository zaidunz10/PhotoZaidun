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
import com.zaidun.photozaidun.data.file.ExportFolderRepository
import androidx.documentfile.provider.DocumentFile
import androidx.work.WorkInfo
import com.zaidun.photozaidun.data.auth.GoogleAuthManager
import com.zaidun.photozaidun.data.drive.DriveServiceFactory
import com.zaidun.photozaidun.data.drive.GoogleDriveRepository
import com.zaidun.photozaidun.domain.model.PhotoItem
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_ACCESS_TOKEN
import com.zaidun.photozaidun.worker.KEY_OUTPUT_FOLDER
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers



data class DriveAuthResult(
    val accessToken: String
)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val driveServiceFactory: DriveServiceFactory,
    private val preferences: UserPreferencesDataStore,
    private val googleAuthManager: GoogleAuthManager,
    private val exportFolderRepository: ExportFolderRepository,
    private val googleDriveRepository: GoogleDriveRepository



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

                if (work.state == WorkInfo.State.SUCCEEDED) {

                    val outputFolder =
                        work.outputData.getString(KEY_OUTPUT_FOLDER)
                            ?: return@observeForever

                    viewModelScope.launch {

                        val prefix = preferences.partFolder.first()


                    }

                }
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
        workManager.cancelAllWorkByTag("UPLOAD")

    }

    fun clearDriveConsent() {

        _uiState.update {

            it.copy(
            )

        }

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
                        .addTag("EXPORT")
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

    fun loadDriveStorage() {


        viewModelScope.launch {

            try {

                val token = preferences.driveAccessToken.first()

                val drive = driveServiceFactory.create(token)

                val storage = withContext(Dispatchers.IO) {

                    googleDriveRepository.getStorageInfo(drive)

                }

                _uiState.update {
                    it.copy(
                        driveStorage = storage,
                        loadingDriveStorage = false
                    )
                }

            }catch (e: Exception) {

                Timber.e(e)

                _uiState.update {

                    it.copy(
                        loadingDriveStorage = false
                    )

                }

            }

        }
    }

    fun initialize(activity: Activity) {

        refreshTokenForUpload(activity) {

            loadDriveStorage()

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

    fun prepareExport(
        accessToken: String
    ) {
        startBatchProcess(accessToken)
    }

    fun refreshTokenForUpload(
        activity: Activity,
        onTokenReady: (String) -> Unit
    ) {

        googleAuthManager.refreshDriveAccessToken(

            activity = activity,

            onTokenReady = { token ->

                viewModelScope.launch {

                    preferences.saveDriveAccessToken(token)

                    loadDriveStorage()

                    onTokenReady(token)

                }

            },

            onNeedConsent = { sender ->

                _uiState.update {

                    it.copy(
                    )

                }

            },

            onError = {

                Timber.e("Refresh token gagal")

            }

        )

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

    fun startBatchProcess(accessToken: String) {

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
        if (inputUri.isEmpty()) return

        val request = OneTimeWorkRequestBuilder<BatchProcessWorker>()
            .addTag("EXPORT")
            .setInputData(
                workDataOf(
                    "input_folder" to inputUri,
                    KEY_ACCESS_TOKEN to accessToken
                )
            )
            .build()

        currentWorkId = request.id
        observeCurrentWorker()
        workManager.enqueue(request)
    }
}

