package com.zaidun.photozaidun.data.drive

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveServiceFactory @Inject constructor() {

    fun create(accessToken: String): Drive {
        val credential = AccessTokenCredential(accessToken)

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setHttpRequestInitializer { request ->
            credential.initialize(request)
            request.connectTimeout = 60000 // 60 Detik Timeout Koneksi
            request.readTimeout = 60000    // 60 Detik Timeout Baca Stream
            request.numberOfRetries = 3    // Auto Retry 3 kali jika jaringan tersendat
        }
        .setApplicationName("Photo Zaidun")
        .build()
    }
}
