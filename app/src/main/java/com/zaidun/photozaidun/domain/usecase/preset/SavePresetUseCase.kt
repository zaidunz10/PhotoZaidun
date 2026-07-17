package com.zaidun.photozaidun.domain.usecase.preset

import com.zaidun.photozaidun.domain.model.Preset
import com.zaidun.photozaidun.domain.repository.PresetRepository
import javax.inject.Inject

class SavePresetUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    suspend operator fun invoke(
        preset: Preset
    ) {
        repository.savePreset(preset)
    }
}