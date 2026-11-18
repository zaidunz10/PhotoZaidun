package com.zaidun.photozaidun.presentation.screen.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaidun.photozaidun.BuildConfig
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
                val token = preferences.driveAccessToken.first()
                if (token.isNotBlank()) {
                    try {
                        val driveService = driveServiceFactory.create(token)
                        val storageInfo = driveRepository.getStorageInfo(driveService)
                        _uiState.update { it.copy(driveStorage = storageInfo) }
                    } catch (e: Exception) {
                        Timber.e(e, "Gagal memuat storage info Google Drive")
                    }
                }

            } catch (e: Exception) {
                Timber.e(e, "Gagal memuat data Dashboard")
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

    fun deleteSession(sessionId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                SupabaseHelper.client.postgrest["sessions"].delete {
                    filter { eq("id", sessionId) }
                }

                _uiState.update { state ->
                    state.copy(
                        mySessions = state.mySessions.filter { it.id != sessionId },
                        isLoading = false
                    )
                }

                Timber.d("Berhasil menghapus sesi ID $sessionId dari Supabase")
                onSuccess()
            } catch (e: Exception) {
                Timber.e(e, "Gagal menghapus sesi $sessionId dari Supabase")
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            }
        }
    }
}
