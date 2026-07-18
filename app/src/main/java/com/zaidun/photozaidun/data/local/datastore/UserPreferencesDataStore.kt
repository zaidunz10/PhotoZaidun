package com.zaidun.photozaidun.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zaidun.photozaidun.data.auth.GoogleUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// Definisi properti ekstensi DataStore
private val Context.dataStore by preferencesDataStore("photozaidun_pref")

class UserPreferencesDataStore @Inject constructor(@dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) {

    companion object {
        // Pindahkan semua kunci ke companion object

        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_EMAIL_KEY = stringPreferencesKey("user_email")

        private val MAIN_FOLDER = stringPreferencesKey("main_folder")
        private val PART_FOLDER = stringPreferencesKey("part_folder")
        private val FILE_SUFFIX = stringPreferencesKey("file_suffix")
        private val MAX_PHOTO_PER_FOLDER = intPreferencesKey("max_photo_per_folder")
        private val WATERMARK_URI = stringPreferencesKey("watermark_uri")
        private val WATERMARK_SCALE = floatPreferencesKey("watermark_scale")
        private val WATERMARK_POSITION = stringPreferencesKey("watermark_position")
        private val WATERMARK_TEXT = stringPreferencesKey("watermark_text")
        private val RESIZE_PERCENT = intPreferencesKey("resize_percent")
        private val JPEG_QUALITY = intPreferencesKey("jpeg_quality")
        private val WATERMARK_OPACITY = floatPreferencesKey("watermark_opacity")
        private val AUTO_UPLOAD = booleanPreferencesKey("auto_upload")
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val LAST_FOLDER = stringPreferencesKey("last_folder")
        private val PREVIEW_IMAGE_URI = stringPreferencesKey("preview_image_uri")

        private val DRIVE_ACCESS_TOKEN = stringPreferencesKey("drive_access_token")

    }
    // Flow untuk mengambil data (pindahkan ke level class agar bisa akses context.dataStore)
    val userId: Flow<String> = context.dataStore.data.map { it[USER_ID_KEY] ?: "" }
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME_KEY] ?: "" }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL_KEY] ?: "" }
    val driveAccessToken: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[DRIVE_ACCESS_TOKEN] ?: ""
        }


    suspend fun saveUser(user: GoogleUser) {
        context.dataStore.edit {
            it[USER_ID_KEY] = user.id
            it[USER_NAME_KEY] = user.name
            it[USER_EMAIL_KEY] = user.email
        }
    }
    suspend fun saveDriveAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[DRIVE_ACCESS_TOKEN] = token
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit {
            it.remove(USER_ID_KEY)
            it.remove(USER_NAME_KEY)
            it.remove(USER_EMAIL_KEY)
        }
    }
    suspend fun saveMainFolder(value: String) {
        context.dataStore.edit { it[MAIN_FOLDER] = value }
    }

    suspend fun savePartFolder(value: String) {
        context.dataStore.edit { it[PART_FOLDER] = value }
    }

    suspend fun saveFileSuffix(value: String) {
        context.dataStore.edit { it[FILE_SUFFIX] = value }
    }

    suspend fun saveMaxPhotoPerFolder(value: Int) {
        context.dataStore.edit { it[MAX_PHOTO_PER_FOLDER] = value }
    }

    suspend fun saveWatermarkText(value: String) {
        context.dataStore.edit { it[WATERMARK_TEXT] = value }
    }

    suspend fun saveWatermarkUri(value: String) {
        context.dataStore.edit { it[WATERMARK_URI] = value }
    }

    suspend fun saveWatermarkScale(value: Float) {
        context.dataStore.edit { it[WATERMARK_SCALE] = value }
    }

    suspend fun saveWatermarkPosition(value: String) {
        context.dataStore.edit { it[WATERMARK_POSITION] = value }
    }

    suspend fun savePreviewImageUri(value: String) {
        context.dataStore.edit { it[PREVIEW_IMAGE_URI] = value }
    }

    suspend fun saveWatermarkOpacity(value: Float) {
        context.dataStore.edit { it[WATERMARK_OPACITY] = value }
    }

    suspend fun saveResizePercent(value: Int) {
        context.dataStore.edit { it[RESIZE_PERCENT] = value }
    }

    suspend fun saveJpegQuality(value: Int) {
        context.dataStore.edit { it[JPEG_QUALITY] = value }
    }

    suspend fun saveDarkMode(value: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = value }
    }

    suspend fun saveAutoUpload(value: Boolean) {
        context.dataStore.edit { it[AUTO_UPLOAD] = value }
    }

    suspend fun saveLastFolder(value: String) {
        context.dataStore.edit { it[LAST_FOLDER] = value }
    }

    // Expose Flows
    val watermarkText: Flow<String> = context.dataStore.data.map { it[WATERMARK_TEXT] ?: "" }
    val previewImageUri: Flow<String> = context.dataStore.data.map { it[PREVIEW_IMAGE_URI] ?: "" }
    val watermarkUri: Flow<String> = context.dataStore.data.map { it[WATERMARK_URI] ?: "" }
    val watermarkScale: Flow<Float> = context.dataStore.data.map { it[WATERMARK_SCALE] ?: 1.0f }
    val mainFolder: Flow<String> = context.dataStore.data.map { it[MAIN_FOLDER] ?: "Customer" }
    val partFolder: Flow<String> = context.dataStore.data.map { it[PART_FOLDER] ?: "part" }
    val fileSuffix: Flow<String> = context.dataStore.data.map { it[FILE_SUFFIX] ?: "" }
    val maxPhotoPerFolder: Flow<Int> = context.dataStore.data.map { it[MAX_PHOTO_PER_FOLDER] ?: 200 }
    val watermarkPosition: Flow<String> = context.dataStore.data.map { it[WATERMARK_POSITION] ?: "BOTTOM_RIGHT" }
    val watermarkOpacity: Flow<Float> = context.dataStore.data.map { it[WATERMARK_OPACITY] ?: 0.8f }
    val resizePercent: Flow<Int> = context.dataStore.data.map { it[RESIZE_PERCENT] ?: 100 }
    val jpegQuality: Flow<Int> = context.dataStore.data.map { it[JPEG_QUALITY] ?: 85 }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }
    val autoUpload: Flow<Boolean> = context.dataStore.data.map { it[AUTO_UPLOAD] ?: false }
    val lastFolder: Flow<String> = context.dataStore.data.map { it[LAST_FOLDER] ?: "" }
}