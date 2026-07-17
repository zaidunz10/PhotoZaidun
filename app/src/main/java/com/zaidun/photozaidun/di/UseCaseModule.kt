package com.zaidun.photozaidun.di

import com.zaidun.photozaidun.domain.repository.HistoryRepository
import com.zaidun.photozaidun.domain.repository.PresetRepository
import com.zaidun.photozaidun.domain.usecase.UseCases
import com.zaidun.photozaidun.domain.usecase.history.GetHistoryUseCase
import com.zaidun.photozaidun.domain.usecase.history.InsertHistoryUseCase
import com.zaidun.photozaidun.domain.usecase.preset.GetAllPresetUseCase
import com.zaidun.photozaidun.domain.usecase.preset.SavePresetUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideUseCases(
        presetRepository: PresetRepository,
        historyRepository: HistoryRepository
    ): UseCases {
        return UseCases(
            getAllPreset = GetAllPresetUseCase(presetRepository),
            savePreset = SavePresetUseCase(presetRepository),
            getHistory = GetHistoryUseCase(historyRepository),
            insertHistory = InsertHistoryUseCase(historyRepository)
        )
    }
}