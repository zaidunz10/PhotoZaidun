package com.zaidun.photozaidun.data.drive

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class DriveServiceFactory @Inject constructor() {

    fun create(
        accessToken: String
    ): Drive {

        return Drive.Builder(

            AndroidHttp.newCompatibleTransport(),

            GsonFactory.getDefaultInstance(),

            AccessTokenCredential(accessToken)

        )
            .setApplicationName("Photo Zaidun")
            .build()

    }

}