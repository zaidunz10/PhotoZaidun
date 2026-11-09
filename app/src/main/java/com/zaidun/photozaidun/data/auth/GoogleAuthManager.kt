package com.zaidun.photozaidun.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.IntentSender
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.zaidun.photozaidun.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import timber.log.Timber
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import org.json.JSONObject
import kotlin.coroutines.suspendCoroutine

data class GoogleUser(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?
)
class GoogleAuthManager@Inject constructor(@ApplicationContext private val context: Context) {
    private val credentialManager = CredentialManager.create(context)


    suspend fun signIn(activityContext: Context): GoogleUser? {
        Timber.d("Login button clicked")

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

                GoogleUser(
                    id = googleCredential.id,
                    name = googleCredential.displayName ?: "",
                    email = googleCredential.id,
                    photoUrl = photoUrl
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Login gagal: ${e.message}")
            throw e
        }
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
            Timber.e(e, "Gagal mengekstrak photoUrl dari JWT ID Token: ${e.message}")
        }

        return null
    }

    suspend fun signOut(activity: Activity) {

        CredentialManager
            .create(activity)
            .clearCredentialState(
                ClearCredentialStateRequest()
            )

    }
    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
    fun refreshDriveAccessToken(
        activity: Activity,
        onTokenReady: (String) -> Unit,
        onNeedConsent: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("https://www.googleapis.com/auth/drive.file")
                )
            )
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { result ->

                if (result.hasResolution()) {

                    result.pendingIntent?.let {
                        onNeedConsent(it.intentSender)
                    }

                } else {

                    val token = result.accessToken

                    if (!token.isNullOrBlank()) {
                        Timber.d("Access Token baru berhasil diperoleh")
                        onTokenReady(token)
                    } else {
                        onError(Exception("Access Token kosong"))
                    }
                }

            }
            .addOnFailureListener(onError)
    }
    fun requestDriveAccess(
        activity: Activity,
        onTokenReady: (String) -> Unit,
        onNeedConsent: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("https://www.googleapis.com/auth/drive.file")
                )
            )
            .build()

        Identity.getAuthorizationClient(activity)
            .authorize(request)
            .addOnSuccessListener { result ->

                if (result.hasResolution()) {

                    result.pendingIntent?.let {
                        onNeedConsent(it.intentSender)
                    }

                } else {

                    val token = result.accessToken

                    if (!token.isNullOrBlank()) {
                        onTokenReady(token)
                    } else {
                        onError(Exception("Access Token kosong"))
                    }

                }

            }
            .addOnFailureListener(onError)
    }

    suspend fun getFreshAccessToken(activity: Activity): String? = suspendCoroutine { cont ->
        refreshDriveAccessToken(
            activity = activity,
            onTokenReady = { token -> cont.resumeWith(Result.success(token)) },
            onNeedConsent = { cont.resumeWith(Result.success(null)) },
            onError = { cont.resumeWith(Result.success(null)) }
        )
    }
}