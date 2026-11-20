package com.zaidun.photozaidun.presentation.screen.dashboard

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.zaidun.photozaidun.BuildConfig
import com.zaidun.photozaidun.data.auth.GoogleAuthManager
import com.zaidun.photozaidun.data.drive.DriveServiceFactory
import com.zaidun.photozaidun.data.drive.GoogleDriveRepository
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.data.remote.SessionLocationDto
import com.zaidun.photozaidun.data.remote.SupabaseHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val preferences: UserPreferencesDataStore,
    private val googleAuthManager: GoogleAuthManager,
    private val driveRepository: GoogleDriveRepository,
    private val driveServiceFactory: DriveServiceFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val userId = preferences.userId.first()
                val email = preferences.userEmail.first()
                val wa = preferences.photographerWa.first()
                val ig = preferences.photographerIg.first()

                _uiState.update {
                    it.copy(
                        photographerEmail = email,
                        photographerWa = wa,
                        photographerIg = ig
                    )
                }

                // 1. Fetch Sessions Fotografer dari Supabase
                if (userId.isNotBlank()) {
                    try {
                        val sessions = SupabaseHelper.client.postgrest["sessions"]
                            .select {
                                filter {
                                    eq("photographer_id", userId)
                                }
                            }.decodeList<SessionLocationDto>()
                            .sortedByDescending { it.id }

                        _uiState.update { it.copy(mySessions = sessions) }
                    } catch (e: Exception) {
                        Timber.e(e, "Gagal memuat sesi fotografer dari Supabase")
                    }
                }

                // 2. Fetch Storage Info Google Drive jika Token Tersedia
                var token = preferences.driveAccessToken.first()
                if (token.isBlank()) {
                    val freshToken = googleAuthManager.getFreshAccessToken()
                    if (!freshToken.isNullOrBlank()) {
                        token = freshToken
                        preferences.saveDriveAccessToken(freshToken)
                    }
                }

                if (token.isNotBlank()) {
                    try {
                        val driveService = driveServiceFactory.create(token)
                        val storageInfo = driveRepository.getStorageInfo(driveService)
                        _uiState.update { it.copy(driveStorage = storageInfo) }
                    } catch (e: Exception) {
                        val is401 = (e is GoogleJsonResponseException && e.statusCode == 401) ||
                                e.message?.contains("401") == true ||
                                e.cause is GoogleJsonResponseException

                        if (is401) {
                            Timber.w("Access Token Drive expired (401), mencoba refresh token secara otomatis...")
                            val freshToken = googleAuthManager.getFreshAccessToken()
                            if (!freshToken.isNullOrBlank()) {
                                preferences.saveDriveAccessToken(freshToken)
                                try {
                                    val newDriveService = driveServiceFactory.create(freshToken)
                                    val storageInfo = driveRepository.getStorageInfo(newDriveService)
                                    _uiState.update { it.copy(driveStorage = storageInfo) }
                                } catch (retryEx: Exception) {
                                    Timber.e(retryEx, "Gagal memuat storage info Google Drive setelah refresh token")
                                    _uiState.update { it.copy(driveStorage = null) }
                                }
                            } else {
                                Timber.w("Refresh token gagal, menghapus token expired")
                                preferences.saveDriveAccessToken("")
                                _uiState.update { it.copy(driveStorage = null) }
                            }
                        } else {
                            Timber.e(e, "Gagal memuat storage info Google Drive")
                            _uiState.update { it.copy(driveStorage = null) }
                        }
                    }
                } else {
                    _uiState.update { it.copy(driveStorage = null) }
                }

            } catch (e: Exception) {
                Timber.e(e, "Gagal memuat data dashboard")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun copyVercelWebLink(context: Context, sessionId: Long) {
        val baseUrl = BuildConfig.WEB_CARIFOTO.trimEnd('/')
        val webLink = "$baseUrl/session/$sessionId"

        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Link Website CariFoto", webLink)
            clipboard.setPrimaryClip(clip)

            _uiState.update { it.copy(copiedSessionId = sessionId) }
            Toast.makeText(context, "Link Vercel Disalin: $webLink", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Timber.e(e, "Gagal menyalin link ke clipboard")
        }
    }

    private fun extractDriveFolderId(urlOrId: String?): String? {
        if (urlOrId.isNullOrBlank()) return null
        if (urlOrId.contains("/folders/")) {
            return urlOrId.split("/folders/")[1].split("?")[0].split("/")[0]
        }
        return urlOrId.takeIf { !it.contains("/") }
    }

    fun deleteSession(
        activity: Activity? = null,
        sessionId: Long,
        driveFolderUrl: String? = null,
        deleteFromDrive: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // 1. Hapus dari Supabase `sessions`
                SupabaseHelper.client.postgrest["sessions"].delete {
                    filter { eq("id", sessionId) }
                }

                // 2. Hapus folder dari Google Drive jika opsi deleteFromDrive diaktifkan (dengan Auto-Refresh Token)
                if (deleteFromDrive && !driveFolderUrl.isNullOrBlank()) {
                    val folderId = extractDriveFolderId(driveFolderUrl)
                    if (folderId != null) {
                        if (activity != null) {
                            googleAuthManager.refreshDriveAccessToken(
                                context = activity,
                                onTokenReady = { freshToken ->
                                    viewModelScope.launch {
                                        preferences.saveDriveAccessToken(freshToken)
                                        try {
                                            val driveService = driveServiceFactory.create(freshToken)
                                            driveRepository.deleteFolder(driveService, folderId)
                                            Timber.d("Google Drive: Folder ID $folderId terhapus dari Drive!")
                                        } catch (e: Exception) {
                                            Timber.e(e, "Gagal menghapus folder di Google Drive")
                                        }
                                    }
                                },
                                onNeedConsent = {},
                                onError = { e -> Timber.e(e, "Gagal refresh token untuk delete Drive") }
                            )
                        } else {
                            var token = preferences.driveAccessToken.first()
                            if (token.isBlank()) {
                                val freshToken = googleAuthManager.getFreshAccessToken()
                                if (!freshToken.isNullOrBlank()) {
                                    token = freshToken
                                    preferences.saveDriveAccessToken(freshToken)
                                }
                            }
                            if (token.isNotBlank()) {
                                try {
                                    val driveService = driveServiceFactory.create(token)
                                    driveRepository.deleteFolder(driveService, folderId)
                                } catch (e: Exception) {
                                    val is401 = (e is GoogleJsonResponseException && e.statusCode == 401) ||
                                            e.message?.contains("401") == true ||
                                            e.cause is GoogleJsonResponseException

                                    if (is401) {
                                        val freshToken = googleAuthManager.getFreshAccessToken()
                                        if (!freshToken.isNullOrBlank()) {
                                            preferences.saveDriveAccessToken(freshToken)
                                            try {
                                                val driveService = driveServiceFactory.create(freshToken)
                                                driveRepository.deleteFolder(driveService, folderId)
                                            } catch (retryEx: Exception) {
                                                Timber.e(retryEx, "Gagal menghapus folder di Google Drive setelah refresh token")
                                            }
                                        }
                                    } else {
                                        Timber.e(e, "Gagal menghapus folder di Google Drive")
                                    }
                                }
                            }
                        }
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        mySessions = state.mySessions.filter { it.id != sessionId },
                        isLoading = false
                    )
                }

                Timber.d("Berhasil menghapus sesi ID $sessionId dari Supabase & Drive=$deleteFromDrive")
                onSuccess()
            } catch (e: Exception) {
                Timber.e(e, "Gagal menghapus sesi $sessionId")
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }
        }
    }
}
