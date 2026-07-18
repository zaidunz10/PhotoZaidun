package com.zaidun.photozaidun.di

import android.content.Context
import com.zaidun.photozaidun.PhotoZaidunApp
import com.zaidun.photozaidun.data.drive.DriveServiceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ): Context {

        return context
    }

    @Provides
    @Singleton
    fun provideDriveServiceFactory(): DriveServiceFactory {

        return DriveServiceFactory()

    }
}