package com.zaidun.photozaidun.data.local.datastore

import android.R.attr.data
import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
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

        private val ROOT_FOLDER = stringPreferencesKey("root_folder")
        private val MAIN_FOLDER = stringPreferencesKey("main_folder")
        private val PART_FOLDER = stringPreferencesKey("part_folder")
        private val FILE_SUFFIX = stringPreferencesKey("file_suffix")
        private val FOLDER_DISPLAY_NAME = stringPreferencesKey("folder_display_name")
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
        private val SHOW_FILENAME = booleanPreferencesKey("show_filename")
        private val SHOW_PART = booleanPreferencesKey("show_part")

        private val DRIVE_ACCESS_TOKEN = stringPreferencesKey("drive_access_token")

        // New Offset & Info Block keys
        private val LOGO_OFFSET_X = floatPreferencesKey("logo_offset_x")
        private val LOGO_OFFSET_Y = floatPreferencesKey("logo_offset_y")
        private val INFO_OFFSET_X = floatPreferencesKey("info_offset_x")
        private val INFO_OFFSET_Y = floatPreferencesKey("info_offset_y")
        private val INFO_FONT_SIZE = floatPreferencesKey("info_font_size")
        private val SHOW_DATE = booleanPreferencesKey("show_date")

        private val SHOW_TIME = booleanPreferencesKey("show_time")

    }

    val userId: Flow<String> = context.dataStore.data.map { it[USER_ID_KEY] ?: "" }
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME_KEY] ?: "" }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL_KEY] ?: "" }
    val folderDisplayName =
        context.dataStore.data.map {
            it[FOLDER_DISPLAY_NAME] ?: ""
        }
    val driveAccessToken: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[DRIVE_ACCESS_TOKEN] ?: ""
        }
    val showDate: Flow<Boolean> =
        context.dataStore.data.map {
            it[SHOW_DATE] ?: true
        }

    val showTime: Flow<Boolean> =
        context.dataStore.data.map {
            it[SHOW_TIME] ?: true
        }
    suspend fun saveShowDate(value: Boolean) {
        context.dataStore.edit {
            it[SHOW_DATE] = value
        }
    }

    suspend fun saveShowTime(value: Boolean) {
        context.dataStore.edit {
            it[SHOW_TIME] = value
        }
    }

    suspend fun saveFolderDisplayName(name: String) {
        context.dataStore.edit {
            it[FOLDER_DISPLAY_NAME] = name
        }
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
    suspend fun saveRootFolder(value: String) { context.dataStore.edit { it[ROOT_FOLDER] = value } }
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
    suspend fun saveShowFilename(value: Boolean) {
        context.dataStore.edit { it[SHOW_FILENAME] = value }
    }
    suspend fun saveShowPart(value: Boolean) {
        context.dataStore.edit { it[SHOW_PART] = value }
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

    suspend fun saveLogoOffset(x: Float, y: Float) {
        context.dataStore.edit {
            it[LOGO_OFFSET_X] = x
            it[LOGO_OFFSET_Y] = y
        }
    }

    suspend fun saveInfoOffset(x: Float, y: Float) {
        context.dataStore.edit {
            it[INFO_OFFSET_X] = x
            it[INFO_OFFSET_Y] = y
        }
    }

    suspend fun saveInfoFontSize(value: Float) {
        context.dataStore.edit { it[INFO_FONT_SIZE] = value }
    }

    // Expose Flows
    val watermarkText: Flow<String> = context.dataStore.data.map { it[WATERMARK_TEXT] ?: "" }
    val showFilename: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[SHOW_FILENAME] ?: true
        }
    val showPart: Flow<Boolean> =
        context.dataStore.data.map { it[SHOW_PART] ?: true }

    val logoOffsetX: Flow<Float> = context.dataStore.data.map { it[LOGO_OFFSET_X] ?: 0.02f }
    val logoOffsetY: Flow<Float> = context.dataStore.data.map { it[LOGO_OFFSET_Y] ?: 0.02f }
    val infoOffsetX: Flow<Float> = context.dataStore.data.map { it[INFO_OFFSET_X] ?: 0.02f }
    val infoOffsetY: Flow<Float> = context.dataStore.data.map { it[INFO_OFFSET_Y] ?: 0.05f }
    val infoFontSize: Flow<Float> = context.dataStore.data.map { it[INFO_FONT_SIZE] ?: 0.04f }

    val rootFolder: Flow<String> = context.dataStore.data.map { it[ROOT_FOLDER] ?: "Folder indux Mu" }
    val previewImageUri: Flow<String> = context.dataStore.data.map { it[PREVIEW_IMAGE_URI] ?: "" }
    val watermarkUri: Flow<String> = context.dataStore.data.map { it[WATERMARK_URI] ?: "" }
    val watermarkScale: Flow<Float> = context.dataStore.data.map { it[WATERMARK_SCALE] ?: 1.0f }
    val mainFolder: Flow<String> = context.dataStore.data.map { it[MAIN_FOLDER] ?: "" }
    val partFolder: Flow<String> = context.dataStore.data.map { it[PART_FOLDER] ?: "" }
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