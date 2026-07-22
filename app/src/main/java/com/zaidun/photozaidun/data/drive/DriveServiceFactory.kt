package com.zaidun.photozaidun.data.drive

import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import jakarta.inject.Inject
import jakarta.inject.Singleton
import com.zaidun.photozaidun.data.drive.DriveServiceFactory

@Singleton
class DriveServiceFactory @Inject constructor(

) {



    fun create(
        accessToken: String
    ): Drive {

        return Drive.Builder(

            com.google.api.client.http.javanet.NetHttpTransport(), // Ganti ke ini
        com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),

            AccessTokenCredential(accessToken)

        )
            .setApplicationName("Photo Zaidun")
            .build()

    }

}