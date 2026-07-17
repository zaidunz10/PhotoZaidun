package com.zaidun.photozaidun.data.local.datastore

import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore by preferencesDataStore("photozaidun_pref")

class UserPreferencesDataStore @Inject constructor(
    private val context: Context
) {

    companion object {
        val WATERMARK_URI =
            stringPreferencesKey("watermark_uri")

        val WATERMARK_SCALE =
            floatPreferencesKey("watermark_scale")

        val WATERMARK_POSITION =
            stringPreferencesKey("watermark_position")

        val WATERMARK_TEXT =
            stringPreferencesKey("watermark_text")

        val RESIZE_PERCENT =
            intPreferencesKey("resize_percent")

        val JPEG_QUALITY =
            intPreferencesKey("jpeg_quality")

        val WATERMARK_OPACITY =
            floatPreferencesKey("watermark_opacity")

        val AUTO_UPLOAD =
            booleanPreferencesKey("auto_upload")

        val DARK_MODE =
            booleanPreferencesKey("dark_mode")

        val LAST_FOLDER =
            stringPreferencesKey("last_folder")
    }

    suspend fun saveWatermarkText(
        value: String
    ) {

        context.dataStore.edit {

            it[WATERMARK_TEXT] = value
        }
    }
    suspend fun saveWatermarkUri(
        value: String
    ) {

        context.dataStore.edit {

            it[WATERMARK_URI] = value

        }

    }

    suspend fun saveWatermarkScale(
        value: Float
    ) {

        context.dataStore.edit {

            it[WATERMARK_SCALE] = value

        }

    }

    suspend fun saveWatermarkPosition(
        value: String
    ) {

        context.dataStore.edit {

            it[WATERMARK_POSITION] = value

        }

    }

    val watermarkText: Flow<String> =
        context.dataStore.data.map {

            it[WATERMARK_TEXT] ?: ""
        }
    val PREVIEW_IMAGE_URI =
        stringPreferencesKey("preview_image_uri")
    suspend fun savePreviewImageUri(value: String) {
        context.dataStore.edit {
            it[PREVIEW_IMAGE_URI] = value
        }
    }
    val previewImageUri: Flow<String> =
        context.dataStore.data.map {
            it[PREVIEW_IMAGE_URI] ?: ""
        }
    val watermarkUri: Flow<String> =
        context.dataStore.data.map {

            it[WATERMARK_URI] ?: ""

        }

    val watermarkScale: Flow<Float> =
        context.dataStore.data.map {

            it[WATERMARK_SCALE] ?: 0.2f

        }
    private val WATERMARK_OFFSET_X =
        floatPreferencesKey("watermark_offset_x")

    private val WATERMARK_OFFSET_Y =
        floatPreferencesKey("watermark_offset_y")
    val watermarkPosition: Flow<String> =
        context.dataStore.data.map {

            it[WATERMARK_POSITION] ?: "BOTTOM_RIGHT"

        }
    suspend fun saveWatermarkOpacity(
        value: Float
    ) {

        context.dataStore.edit {

            it[WATERMARK_OPACITY] = value

        }


    }
    val watermarkOpacity: Flow<Float> =
        context.dataStore.data.map {

            it[WATERMARK_OPACITY] ?: 0.8f

        }

    suspend fun saveResizePercent(
        value: Int
    ) {

        context.dataStore.edit {

            it[RESIZE_PERCENT] = value
        }
    }

    val resizePercent: Flow<Int> =
        context.dataStore.data.map {

            it[RESIZE_PERCENT] ?: 100
        }

    suspend fun saveJpegQuality(
        value: Int
    ) {

        context.dataStore.edit {

            it[JPEG_QUALITY] = value
        }
    }

    val jpegQuality: Flow<Int> =
        context.dataStore.data.map {

            it[JPEG_QUALITY] ?: 85
        }

    suspend fun saveDarkMode(
        value: Boolean
    ) {

        context.dataStore.edit {

            it[DARK_MODE] = value
        }
    }

    val darkMode: Flow<Boolean> =
        context.dataStore.data.map {

            it[DARK_MODE] ?: false
        }

    suspend fun saveAutoUpload(
        value: Boolean
    ) {

        context.dataStore.edit {

            it[AUTO_UPLOAD] = value
        }
    }

    val autoUpload: Flow<Boolean> =
        context.dataStore.data.map {

            it[AUTO_UPLOAD] ?: false
        }

    suspend fun saveLastFolder(
        value: String
    ) {

        context.dataStore.edit {

            it[LAST_FOLDER] = value
        }
    }

    val lastFolder: Flow<String> =
        context.dataStore.data.map {

            it[LAST_FOLDER] ?: ""
        }
}