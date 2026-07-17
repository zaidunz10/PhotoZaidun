package com.zaidun.photozaidun.di

import android.content.Context
import androidx.room.Room
import com.zaidun.photozaidun.data.local.database.PhotoZaidunDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PhotoZaidunDatabase {

        return Room.databaseBuilder(
            context,
            PhotoZaidunDatabase::class.java,
            "photozaidun.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePresetDao(
        database: PhotoZaidunDatabase
    ) = database.presetDao()

    @Provides
    @Singleton
    fun provideHistoryDao(
        database: PhotoZaidunDatabase
    ) = database.processHistoryDao()

    @Provides
    @Singleton
    fun provideProcessedFileDao(
        database: PhotoZaidunDatabase
    ) = database.processedFileDao()

    @Provides
    @Singleton
    fun provideWatermarkDao(
        database: PhotoZaidunDatabase
    ) = database.watermarkTemplateDao()
}