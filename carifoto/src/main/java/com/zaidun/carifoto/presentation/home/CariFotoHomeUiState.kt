
package com.zaidun.carifoto.presentation.home

import com.zaidun.carifoto.data.remote.CariFotoPhotoDto
import com.zaidun.carifoto.data.remote.CariFotoSessionDto
import com.zaidun.carifoto.data.remote.CariFotoUserDto
import com.zaidun.carifoto.data.model.DrivePhotoItem

data class CariFotoHomeUiState(
    val selectedLocation: String = "",
    val availableLocations: List<String> = emptyList(),
    val drivePhotos: List<DrivePhotoItem> = emptyList(),
    val filteredDrivePhotos: List<DrivePhotoItem> = emptyList(),
    val selectedDate: String = "",
    val availableDates: List<String> = emptyList(),

    val selectedHourRange: String = "",
    val availableHourRanges: List<String> = emptyList(),

    val selectedSessionId: Long? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,

    val users: List<CariFotoUserDto> = emptyList(),
    val sessions: List<CariFotoSessionDto> = emptyList(),
    val photos: List<CariFotoPhotoDto> = emptyList(),
    val filteredPhotos: List<CariFotoPhotoDto> = emptyList(),

    val selectedPhotoForPayment: CariFotoPhotoDto? = null,
    val selectedSessionForPayment: CariFotoSessionDto? = null,
    val showPaymentSheet: Boolean = false,

    val purchasedPhotoIds: Set<Long> = emptySet(),
    val riderId: String = ""
)
