package com.zaidun.photozaidun.data.auth

import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import jakarta.inject.Inject

class TokenRefresher @Inject constructor(
    private val googleAuthManager: GoogleAuthManager,
    private val preferences: UserPreferencesDataStore
) {

}