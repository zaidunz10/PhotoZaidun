package com.zaidun.carifoto.data.auth

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.zaidun.carifoto.BuildConfig
import org.json.JSONObject
import timber.log.Timber

data class CariFotoGoogleUser(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?
)

class CariFotoGoogleAuthManager(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(activityContext: Context): CariFotoGoogleUser? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val photoUrl = extractGooglePhotoUrl(googleCredential)

                CariFotoGoogleUser(
                    id = googleCredential.id,
                    name = googleCredential.displayName ?: "",
                    email = googleCredential.id,
                    photoUrl = photoUrl
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Login Google gagal. Message: ${e.message}")
            throw e
        }
    }

    suspend fun getAuthorizedUserSilently(activityContext: Context): CariFotoGoogleUser? {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(BuildConfig.WEB_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val photoUrl = extractGooglePhotoUrl(googleCredential)

                CariFotoGoogleUser(
                    id = googleCredential.id,
                    name = googleCredential.displayName ?: "",
                    email = googleCredential.id,
                    photoUrl = photoUrl
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun signOut(activity: Activity) {
        CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
    }

    private fun extractGooglePhotoUrl(googleCredential: GoogleIdTokenCredential): String? {
        val directUri = googleCredential.profilePictureUri?.toString()
        if (!directUri.isNullOrBlank()) return directUri

        try {
            val idToken = googleCredential.idToken
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val payloadBytes = Base64.decode(
                    parts[1],
                    Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
                )
                val payloadJson = String(payloadBytes, Charsets.UTF_8)
                val jsonObject = JSONObject(payloadJson)
                if (jsonObject.has("picture")) {
                    val picUrl = jsonObject.getString("picture")
                    if (picUrl.isNotBlank()) return picUrl
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Gagal mengekstrak photoUrl dari JWT")
        }

        return null
    }
}
