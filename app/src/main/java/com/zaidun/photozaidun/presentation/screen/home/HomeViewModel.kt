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
import com.zaidun.photozaidun.data.remote.UserLogger
import com.zaidun.photozaidun.worker.BatchProcessWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.content.Intent
import com.zaidun.photozaidun.data.file.ExportFolderRepository
import androidx.documentfile.provider.DocumentFile
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.zaidun.photozaidun.data.auth.GoogleAuthManager
import com.zaidun.photozaidun.data.drive.DriveServiceFactory
import com.zaidun.photozaidun.data.drive.GoogleDriveRepository
import com.zaidun.photozaidun.data.remote.SessionInsertDto
import com.zaidun.photozaidun.data.remote.SessionLocationDto
import com.zaidun.photozaidun.data.remote.SessionResponseDto
import com.zaidun.photozaidun.data.remote.SupabaseHelper
import com.zaidun.photozaidun.data.remote.UserDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.zaidun.photozaidun.domain.model.PhotoItem
import io.github.jan.supabase.postgrest.postgrest
import com.zaidun.photozaidun.worker.DriveUploadWorker.Companion.KEY_ACCESS_TOKEN
import com.zaidun.photozaidun.worker.KEY_OUTPUT_FOLDER
import io.github.jan.supabase.postgrest.query.Columns
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

    init {
        viewModelScope.launch {
            launch { preferences.userId.collect { id -> _uiState.update { it.copy(userId = id) } } }
            launch {
                preferences.userEmail.collect { email ->
                    _uiState.update { it.copy(userEmail = email) }
                    if (email.isNotBlank()) {
                        loadUserProfileFromSupabase(email)
                    }
                }
            }
            launch { preferences.userName.collect { name -> _uiState.update { it.copy(userName = name) } } }
            launch { preferences.userPhotoUrl.collect { photo -> _uiState.update { it.copy(userPhotoUrl = photo) } } }
            launch { preferences.photographerWa.collect { wa -> _uiState.update { it.copy(photographerWa = wa) } } }
            launch { preferences.photographerIg.collect { ig -> _uiState.update { it.copy(photographerIg = ig) } } }
        }
    }

    fun loadUserProfileFromSupabase(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            try {
                val users = SupabaseHelper.client.postgrest["users"]
                    .select {
                        filter { eq("email", email) }
                    }
                    .decodeList<UserDto>()

                val user = users.firstOrNull()
                if (user != null && !user.avatarUrl.isNullOrBlank()) {
                    _uiState.update { it.copy(userPhotoUrl = user.avatarUrl) }
                    preferences.saveUserPhotoUrl(user.avatarUrl)
                }
            } catch (e: Exception) {
                Timber.e(e, "Supabase: Gagal mengambil foto profil user")
            }
        }
    }
    private fun observeCurrentWorker() {

        val id = currentWorkId ?: return

        workManager
            .getWorkInfoByIdLiveData(id)
            .observeForever { work ->
        Timber.tag("OBSERVE").d("Observe ID = $id")
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

        workManager.cancelUniqueWork("BATCH_EXPORT_TASK")
        workManager.cancelAllWorkByTag("EXPORT")
        workManager.cancelAllWorkByTag("UPLOAD")

        _uiState.update {
            it.copy(
                isProcessing = false,
                progress = 0f,
                current = 0,
                total = 0,
                currentFilename = ""
            )
        }
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
                    triggerExportLog()

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
                var token = preferences.driveAccessToken.first()
                if (token.isBlank()) {
                    val freshToken = googleAuthManager.getFreshAccessToken()
                    if (!freshToken.isNullOrBlank()) {
                        token = freshToken
                        preferences.saveDriveAccessToken(freshToken)
                    }
                }

                if (token.isNotBlank()) {
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
                } else {
                    _uiState.update {
                        it.copy(
                            driveStorage = null,
                            loadingDriveStorage = false
                        )
                    }
                }
            } catch (e: Exception) {
                val is401 = (e is GoogleJsonResponseException && e.statusCode == 401) ||
                        e.message?.contains("401") == true ||
                        e.cause is GoogleJsonResponseException

                if (is401) {
                    try {
                        val freshToken = googleAuthManager.getFreshAccessToken()
                        if (!freshToken.isNullOrBlank()) {
                            preferences.saveDriveAccessToken(freshToken)
                            val drive = driveServiceFactory.create(freshToken)
                            val storage = withContext(Dispatchers.IO) {
                                googleDriveRepository.getStorageInfo(drive)
                            }
                            _uiState.update {
                                it.copy(
                                    driveStorage = storage,
                                    loadingDriveStorage = false
                                )
                            }
                            return@launch
                        } else {
                            preferences.saveDriveAccessToken("")
                        }
                    } catch (retryEx: Exception) {
                        Timber.e(retryEx, "Gagal memuat storage info Google Drive setelah refresh token")
                    }
                }
                Timber.e(e, "Gagal memuat storage info Google Drive")
                _uiState.update {
                    it.copy(
                        loadingDriveStorage = false,
                        driveStorage = null
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
        startBatchProcess(accessToken, true) // Default tanpa Session ID jika langsung dari UI
    }

    fun refreshTokenForUpload(
        activity: Activity,
        onTokenReady: (String) -> Unit
    ) {

        googleAuthManager.refreshDriveAccessToken(

            context = activity,

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
            // Memaksa buka aplikasi Drive jira ada
            setPackage("com.google.android.apps.docs")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Jika app Drive tidak ada, buka via Browser
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://drive.google.com")))
        }
    }

    private fun triggerExportLog() {
        val email = _uiState.value.userEmail
        val name = _uiState.value.userName
        val total = _uiState.value.totalImages
        Timber.tag("UserLogger").d("Trigger Export Log: email=$email, name=$name, total=$total")
        UserLogger.logExport(email, name, total)
    }

    fun loadLocationSuggestionsFromSupabase() {
        viewModelScope.launch {
            try {
                val dbLocations = SupabaseHelper.client.postgrest["sessions"]
                    .select(Columns.list("location"))
                    .decodeList<SessionLocationDto>()
                    .map { it.location.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()

                if (dbLocations.isNotEmpty()) {
                    _uiState.update { state ->
                        val combined = (state.locationSuggestions + dbLocations).distinct()
                        state.copy(locationSuggestions = combined)
                    }
                    Timber.d("Supabase: Berhasil memuat ${dbLocations.size} lokasi unik dari database.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Supabase: Gagal mengambil daftar lokasi")
            }
        }
    }

    fun onStartExportClicked(): Boolean {
        // Cek kelengkapan WA & IG dari DataStore (sebelumnya diisi di ProfileScreen)
        val wa = _uiState.value.photographerWa
        val ig = _uiState.value.photographerIg
        if (wa.isBlank() || ig.isBlank()) {
            return false // Minta user lengkapi profil dulu
        }
        
        loadLocationSuggestionsFromSupabase()
        // Profil lengkap, tampilkan dialog lokasi
        _uiState.update { it.copy(showLocationDialog = true) }
        return true
    }

    fun dismissLocationDialog() {
        _uiState.update { it.copy(showLocationDialog = false) }
    }

    fun togglePublishForRider(enabled: Boolean) {
        _uiState.update { it.copy(isPublishedForRider = enabled) }
    }
    
    fun confirmLocationAndExport(location: String, accessToken: String) {
        dismissLocationDialog()
        startBatchProcess(accessToken, _uiState.value.isPublishedForRider, location.trim())
    }

    private fun startBatchProcess(accessToken: String, isPublished: Boolean, location: String = "") {
        if (_uiState.value.isProcessing) {
            Timber.tag("EXPORT").d("Proses sedang berjalan, mengabaikan klik ganda.")
            return
        }

        Timber.tag("EXPORT").e("========== START BATCH ==========")
        triggerExportLog()

        val request = OneTimeWorkRequestBuilder<BatchProcessWorker>()
            .addTag("EXPORT")
            .setInputData(
                workDataOf(
                    "input_folder" to _uiState.value.selectedFolder, 
                    KEY_ACCESS_TOKEN to accessToken,
                    "is_published" to isPublished,
                    "location" to location
                )
            )
            .build()

        currentWorkId = request.id
        observeCurrentWorker()

        // GUNAKAN UNIQUE WORK
        workManager.enqueueUniqueWork(
            "BATCH_EXPORT_TASK",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

