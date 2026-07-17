package com.zaidun.photozaidun.domain.repository

import com.zaidun.photozaidun.domain.model.Preset
import kotlinx.coroutines.flow.Flow

interface PresetRepository {

    fun getAllPreset(): Flow<List<Preset>>

    suspend fun savePreset(
        preset: Preset
    )

    suspend fun deletePreset(
        preset: Preset
    )
}