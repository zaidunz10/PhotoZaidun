package com.zaidun.photozaidun.data.remote

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import timber.log.Timber

class GoogleAuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): String? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("446637170765-4o09lfdahjij4j4et9fsrijdjvv6g882.apps.googleusercontent.com") // Ganti dengan Client ID dari Google Cloud Console
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            Timber.d("Login Berhasil")
            "SUCCESS_TOKEN"
        } catch (e: Exception) {
            Timber.e(e, "Login Gagal")
            null
        }
    }

    suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}