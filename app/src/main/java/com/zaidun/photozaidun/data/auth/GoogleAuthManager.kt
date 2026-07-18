package com.zaidun.photozaidun.data.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.IntentSender
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


                val googleCredential =
                    GoogleIdTokenCredential.createFrom(
                        credential.data
                    )

                GoogleUser(
                    id = googleCredential.id,
                    name = googleCredential.displayName ?: "",
                    email = googleCredential.id,
                    photoUrl = googleCredential.profilePictureUri?.toString()
                )

            } else {

                null

            }

        } catch (e: Exception) {
            Timber.e(e, "Login gagal: ${e.message}")
            throw e
        }

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
    fun requestDriveAccess(
        activity: Activity,
        onTokenReady: (String) -> Unit,
        onNeedConsent: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(Scopes.DRIVE_FILE)
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
    fun requestDrivePermission(
        activity: Activity,
        onSuccess: () -> Unit,
        onNeedUserConsent: (IntentSender) -> Unit,
        onError: (Exception) -> Unit
    ) {

        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(Scopes.DRIVE_FILE)
                )
            )
            .build()

        Identity
            .getAuthorizationClient(activity)
            .authorize(authorizationRequest)
            .addOnSuccessListener { result ->

                if (result.hasResolution()) {

                    result.pendingIntent?.let {

                        onNeedUserConsent(
                            it.intentSender
                        )

                    }

                } else {

                    Timber.d("Drive permission sudah tersedia")

                    onSuccess()

                }

            }
            .addOnFailureListener {
                onError(it)
            }

    }
}