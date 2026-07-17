package com.zaidun.photozaidun.domain.usecase.preset

import com.zaidun.photozaidun.domain.repository.PresetRepository
import javax.inject.Inject

class GetAllPresetUseCase @Inject constructor(
    private val repository: PresetRepository
) {
    operator fun invoke() = repository.getAllPreset()
}