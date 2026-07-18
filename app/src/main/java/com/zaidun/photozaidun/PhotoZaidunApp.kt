package com.zaidun.photozaidun

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class PhotoZaidunApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory



    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())

        android.util.Log.e("PHOTOZAIDUN", "PhotoZaidunApp onCreate")
    }
    override val workManagerConfiguration: Configuration
        get() {
            android.util.Log.e("PHOTOZAIDUN", "Using HiltWorkerFactory = $workerFactory")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build()
        }
}