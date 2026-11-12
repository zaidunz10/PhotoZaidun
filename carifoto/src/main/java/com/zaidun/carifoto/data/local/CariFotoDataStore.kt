package com.zaidun.carifoto.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zaidun.carifoto.data.auth.CariFotoGoogleUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("carifoto_rider_pref")

class CariFotoDataStore(private val context: Context) {

    companion object {
        private val RIDER_ID_KEY = stringPreferencesKey("rider_user_id")
        private val RIDER_NAME_KEY = stringPreferencesKey("rider_user_name")
        private val RIDER_EMAIL_KEY = stringPreferencesKey("rider_user_email")
        private val RIDER_PHOTO_URL_KEY = stringPreferencesKey("rider_user_photo_url")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("rider_is_logged_in")
    }

    val riderUser: Flow<CariFotoGoogleUser?> = context.dataStore.data.map { prefs ->
        val id = prefs[RIDER_ID_KEY] ?: ""
        val name = prefs[RIDER_NAME_KEY] ?: ""
        val email = prefs[RIDER_EMAIL_KEY] ?: ""
        val photoUrl = prefs[RIDER_PHOTO_URL_KEY] ?: ""
        val isLoggedIn = prefs[IS_LOGGED_IN_KEY] ?: false

        if (isLoggedIn && id.isNotBlank()) {
            CariFotoGoogleUser(id = id, name = name, email = email, photoUrl = photoUrl.ifBlank { null })
        } else {
            null
        }
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN_KEY] ?: false
    }

    suspend fun saveUser(user: CariFotoGoogleUser) {
        context.dataStore.edit { prefs ->
            prefs[RIDER_ID_KEY] = user.id
            prefs[RIDER_NAME_KEY] = user.name
            prefs[RIDER_EMAIL_KEY] = user.email
            prefs[RIDER_PHOTO_URL_KEY] = user.photoUrl ?: ""
            prefs[IS_LOGGED_IN_KEY] = true
        }
    }

    suspend fun saveGuestUser() {
        context.dataStore.edit { prefs ->
            prefs[RIDER_ID_KEY] = "guest_rider_001"
            prefs[RIDER_NAME_KEY] = "Pengendara (Tamu)"
            prefs[RIDER_EMAIL_KEY] = "guest@carifoto.id"
            prefs[RIDER_PHOTO_URL_KEY] = ""
            prefs[IS_LOGGED_IN_KEY] = true
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit { prefs ->
            prefs.remove(RIDER_ID_KEY)
            prefs.remove(RIDER_NAME_KEY)
            prefs.remove(RIDER_EMAIL_KEY)
            prefs.remove(RIDER_PHOTO_URL_KEY)
            prefs[IS_LOGGED_IN_KEY] = false
        }
    }
}
