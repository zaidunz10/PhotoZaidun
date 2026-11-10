package com.zaidun.carifoto.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaidun.carifoto.data.remote.CariFotoOrderDto
import com.zaidun.carifoto.data.remote.CariFotoPhotoDto
import com.zaidun.carifoto.data.remote.CariFotoSessionDto
import com.zaidun.carifoto.data.remote.CariFotoSupabaseHelper
import com.zaidun.carifoto.data.remote.CariFotoUserDto
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.zaidun.carifoto.data.repository.CariFotoDriveRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CariFotoHomeViewModel : ViewModel() {
    private val driveRepository = CariFotoDriveRepository()
    private val _uiState = MutableStateFlow(CariFotoHomeUiState())

    val uiState: StateFlow<CariFotoHomeUiState> = _uiState.asStateFlow()

    init {
        loadDataFromSupabase()
    }
    fun setRiderId(riderId: String) {
        _uiState.update {
            it.copy(riderId = riderId)
        }
    }
    fun loadDataFromSupabase() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                // 1. Fetch `users` dari Supabase
                val users = try {
                    CariFotoSupabaseHelper.client.postgrest["users"]
                        .select()
                        .decodeList<CariFotoUserDto>()
                } catch (e: Exception) {
                    Timber.e(e, "Gagal decode users dari Supabase")
                    emptyList()
                }

                // 2. Fetch `sessions` dari Supabase
                val sessions = try {
                    CariFotoSupabaseHelper.client.postgrest["sessions"]
                        .select()
                        .decodeList<CariFotoSessionDto>()
                } catch (e: Exception) {
                    Timber.e(e, "Gagal decode sessions dari Supabase")
                    emptyList()
                }

                val photos = emptyList<CariFotoPhotoDto>()
                val locations = sessions.map { it.location.trim() }.filter { it.isNotBlank() }.distinct()
                val dates = sessions.mapNotNull { it.sessionDate?.take(10) }.filter { it.isNotBlank() }.distinct()

                val hourRanges = listOf(
                    "06.00 - 07.00",
                    "07.00 - 08.00",
                    "08.00 - 09.00",
                    "09.00 - 10.00",
                    "10.00 - 11.00",
                    "11.00 - 12.00",
                    "12.00 - 13.00",
                    "13.00 - 14.00",
                    "14.00 - 15.00",
                    "15.00 - 16.00",
                    "16.00 - 17.00",
                    "17.00 - 18.00"
                )

                _uiState.update { state ->
                    val defaultLoc = if (locations.isNotEmpty()) locations.first() else ""
                    val defaultDate = if (dates.isNotEmpty()) dates.first() else ""
                    val defaultHour = hourRanges.first()

                    state.copy(
                        users = users,
                        sessions = sessions,
                        photos = photos,
                        availableLocations = locations,
                        availableDates = dates,
                        availableHourRanges = hourRanges,
                        selectedLocation = if (state.selectedLocation.isBlank()) defaultLoc else state.selectedLocation,
                        selectedDate = if (state.selectedDate.isBlank()) defaultDate else state.selectedDate,
                        selectedHourRange = if (state.selectedHourRange.isBlank()) defaultHour else state.selectedHourRange,
                        isLoading = false
                    )
                }

                applyFilter()

            } catch (e: Exception) {
                Timber.e(e, "Gagal memuat data dari Supabase")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }


    fun loadPhotosFromDrive(sessionId: Long) {
        viewModelScope.launch {
            try {
                Timber.d("DRIVE: Mulai mengambil sesi $sessionId")

                _uiState.update {
                    it.copy(
                        isLoading = true,
                        drivePhotos = emptyList(),
                        filteredDrivePhotos = emptyList()
                    )
                }

                val session = _uiState.value.sessions
                    .firstOrNull { it.id == sessionId }

                if (session == null) {
                    Timber.e("DRIVE: Sesi $sessionId tidak ditemukan")
                    throw Exception("Sesi tidak ditemukan")
                }

                Timber.d("DRIVE: Sesi ditemukan: ${session.location}")

                val folderUrl = session.driveFolderId

                Timber.d("DRIVE: Folder URL = $folderUrl")

                if (folderUrl.isNullOrBlank()) {
                    throw Exception("Link folder Google Drive kosong")
                }

                val photos = driveRepository.getPhotos(folderUrl)

                Timber.d("DRIVE: Total foto ditemukan = ${photos.size}")

                photos.forEach {
                    Timber.d("DRIVE FOTO: ${it.name} | ${it.id}")
                }

                _uiState.update {
                    it.copy(
                        drivePhotos = photos,
                        filteredDrivePhotos = photos,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                Timber.e(e, "DRIVE ERROR: Gagal mengambil foto")

                _uiState.update {
                    it.copy(
                        drivePhotos = emptyList(),
                        filteredDrivePhotos = emptyList(),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun applyDrivePhotoFilter() {
        val state = _uiState.value
        val query = state.searchQuery.trim()

        val filtered = state.drivePhotos.filter { photo ->
            query.isBlank() ||
                    photo.name.contains(query, ignoreCase = true)
        }

        _uiState.update {
            it.copy(filteredDrivePhotos = filtered)
        }
    }

    fun selectLocation(location: String) {
        _uiState.update {
            it.copy(
                selectedLocation = location,
                selectedSessionId = null,
                drivePhotos = emptyList(),
                filteredDrivePhotos = emptyList()
            )
        }
        applyFilter()
    }

    fun selectDate(date: String) {
        _uiState.update {
            it.copy(
                selectedDate = date,
                selectedSessionId = null,
                drivePhotos = emptyList(),
                filteredDrivePhotos = emptyList()
            )
        }
        applyFilter()
    }


    fun selectHourRange(hourRange: String) {
        _uiState.update {
            it.copy(selectedHourRange = hourRange)
        }

        applyFilter()
    }

    fun selectSession(sessionId: Long?) {
        Timber.d("SESSION DIPILIH: $sessionId")

        _uiState.update {
            it.copy(
                selectedSessionId = sessionId,
                searchQuery = "",
                selectedHourRange = "",
                drivePhotos = emptyList(),
                filteredDrivePhotos = emptyList()
            )
        }

        if (sessionId != null) {
            loadPhotosFromDrive(sessionId)
        }
    }


    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(searchQuery = query)
        }

        applyFilter()
        applyDrivePhotoFilter()
    }


    private fun applyFilter() {
        val state = _uiState.value
        val activeSessionId = state.selectedSessionId
        val query = state.searchQuery.trim()

        // Belum memilih folder, jadi belum menampilkan foto.
        if (activeSessionId == null) {
            _uiState.update {
                it.copy(filteredPhotos = emptyList())
            }
            return
        }

        val filtered = state.photos.filter { photo ->
            val matchSession = photo.sessionId == activeSessionId

            val matchQuery = query.isBlank() ||
                    photo.fileName.contains(query, ignoreCase = true)

            val matchHour = if (state.selectedHourRange.isNotBlank()) {
                val startHour = state.selectedHourRange
                    .take(2)
                    .toIntOrNull()

                val photoHour = photo.exifTime
                    ?.take(2)
                    ?.toIntOrNull()

                startHour != null &&
                        photoHour != null &&
                        photoHour == startHour
            } else {
                true
            }

            matchSession && matchQuery && matchHour
        }

        _uiState.update {
            it.copy(filteredPhotos = filtered)
        }
    }


    fun openPaymentSheet(photo: CariFotoPhotoDto) {
        val session = _uiState.value.sessions.find { it.id == photo.sessionId }
        _uiState.update {
            it.copy(
                selectedPhotoForPayment = photo,
                selectedSessionForPayment = session,
                showPaymentSheet = true
            )
        }
    }

    fun dismissPaymentSheet() {
        _uiState.update {
            it.copy(
                showPaymentSheet = false,
                selectedPhotoForPayment = null,
                selectedSessionForPayment = null
            )
        }
    }

    fun processQrisPayment(onSuccess: () -> Unit) {
        val state = _uiState.value
        val photo = state.selectedPhotoForPayment ?: return
        val session = state.selectedSessionForPayment

        val price = session?.priceSingle ?: 10000
        val adminFee = 2000
        val total = price + adminFee

        viewModelScope.launch {
            try {
                val order = CariFotoOrderDto(
                    riderId = state.riderId,
                    photoId = photo.id,
                    price = price,
                    adminFee = adminFee,
                    totalPrice = total,
                    status = "PAID"
                )

                CariFotoSupabaseHelper.client.postgrest["orders"].insert(order)

                _uiState.update {
                    it.copy(
                        purchasedPhotoIds = it.purchasedPhotoIds + photo.id,
                        showPaymentSheet = false,
                        selectedPhotoForPayment = null
                    )
                }

                Timber.d("Pembayaran QRIS sukses! Photo ID ${photo.id} ditambahkan ke koleksi.")
                onSuccess()
            } catch (e: Exception) {
                Timber.e(e, "Gagal insert order ke Supabase")
                // Tetap simpan lokal agar user bisa download jika offline
                _uiState.update {
                    it.copy(
                        purchasedPhotoIds = it.purchasedPhotoIds + photo.id,
                        showPaymentSheet = false,
                        selectedPhotoForPayment = null
                    )
                }
                onSuccess()
            }
        }
    }
}
