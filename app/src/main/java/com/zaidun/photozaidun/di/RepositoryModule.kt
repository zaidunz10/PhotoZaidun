package com.zaidun.photozaidun.di

import android.content.Context
import com.zaidun.photozaidun.data.local.dao.PresetDao
import com.zaidun.photozaidun.data.local.dao.ProcessHistoryDao
import com.zaidun.photozaidun.data.repository.HistoryRepositoryImpl
import com.zaidun.photozaidun.data.repository.ImageScannerRepositoryImpl
import com.zaidun.photozaidun.data.repository.PresetRepositoryImpl
import com.zaidun.photozaidun.data.source.local.scanner.ImageScanner
import com.zaidun.photozaidun.data.source.local.scanner.SafImageScanner
import com.zaidun.photozaidun.domain.repository.HistoryRepository
import com.zaidun.photozaidun.domain.repository.ImageScannerRepository
import com.zaidun.photozaidun.domain.repository.PresetRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePresetRepository(
        dao: PresetDao
    ): PresetRepository =
        PresetRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideHistoryRepository(
        dao: ProcessHistoryDao
    ): HistoryRepository =
        HistoryRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideImageScanner(

        @ApplicationContext context: Context

    ): ImageScanner {

        return SafImageScanner(context)

    }

    @Provides
    @Singleton
    fun provideImageScannerRepository(

        scanner: ImageScanner

    ): ImageScannerRepository {

        return ImageScannerRepositoryImpl(scanner)

    }
}