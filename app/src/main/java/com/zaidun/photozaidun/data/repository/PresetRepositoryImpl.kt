package com.zaidun.photozaidun.data.repository

import com.zaidun.photozaidun.data.local.dao.PresetDao
import com.zaidun.photozaidun.data.mapper.toDomain
import com.zaidun.photozaidun.data.mapper.toEntity
import com.zaidun.photozaidun.domain.model.Preset
import com.zaidun.photozaidun.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PresetRepositoryImpl @Inject constructor(
    private val dao: PresetDao
) : PresetRepository {

    override fun getAllPreset(): Flow<List<Preset>> {
        return dao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun savePreset(
        preset: Preset
    ) {
        dao.insert(preset.toEntity())
    }

    override suspend fun deletePreset(
        preset: Preset
    ) {
        dao.delete(preset.toEntity())
    }
}