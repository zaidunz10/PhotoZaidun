package com.zaidun.photozaidun.domain.usecase

import com.zaidun.photozaidun.domain.usecase.history.GetHistoryUseCase
import com.zaidun.photozaidun.domain.usecase.history.InsertHistoryUseCase
import com.zaidun.photozaidun.domain.usecase.preset.GetAllPresetUseCase
import com.zaidun.photozaidun.domain.usecase.preset.SavePresetUseCase

data class UseCases(

    val getAllPreset: GetAllPresetUseCase,

    val savePreset: SavePresetUseCase,

    val getHistory: GetHistoryUseCase,

    val insertHistory: InsertHistoryUseCase
)