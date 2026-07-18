package com.zaidun.photozaidun.data.drive

import com.google.api.client.http.HttpRequest
import com.google.api.client.http.HttpRequestInitializer

class AccessTokenCredential(
    private val accessToken: String
) : HttpRequestInitializer {

    override fun initialize(request: HttpRequest) {

        request.headers.authorization =
            "Bearer $accessToken"

    }

}