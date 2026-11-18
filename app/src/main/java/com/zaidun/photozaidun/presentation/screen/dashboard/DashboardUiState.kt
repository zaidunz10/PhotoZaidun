package com.zaidun.photozaidun.presentation.screen.dashboard

import com.zaidun.photozaidun.data.remote.SessionLocationDto
import com.zaidun.photozaidun.domain.model.DriveStorageInfo

data class DashboardUiState(
    val isLoading: Boolean = false,
    val driveStorage: DriveStorageInfo? = null,
    val mySessions: List<SessionLocationDto> = emptyList(),
    val photographerEmail: String = "",
    val photographerWa: String = "",
    val photographerIg: String = "",
    val copiedSessionId: Long? = null
)
