package com.zaidun.photozaidun.data.remote.gemini

import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiKeyManager @Inject constructor(
    private val preferences: UserPreferencesDataStore
) {
    private val roundRobinIndex = AtomicInteger(0)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getKeys(): List<GeminiApiKeyItem> {
        return preferences.geminiApiKeys.first()
    }

    suspend fun addApiKey(label: String, apiKey: String): GeminiApiKeyItem {
        val currentKeys = getKeys().toMutableList()
        val cleanLabel = label.trim().ifBlank { "Akun ${currentKeys.size + 1}" }
        val newItem = GeminiApiKeyItem(
            id = UUID.randomUUID().toString(),
            label = cleanLabel,
            apiKey = apiKey.trim(),
            isActive = true,
            isQuotaExceeded = false
        )
        currentKeys.add(newItem)
        preferences.saveGeminiApiKeys(currentKeys)
        Timber.d("GeminiKeyManager: Berhasil menambahkan API Key '${newItem.label}'")
        return newItem
    }

    suspend fun removeApiKey(id: String) {
        val currentKeys = getKeys().filterNot { it.id == id }
        preferences.saveGeminiApiKeys(currentKeys)
        Timber.d("GeminiKeyManager: Berhasil menghapus API Key ID '$id'")
    }

    suspend fun toggleKeyActive(id: String, active: Boolean) {
        val currentKeys = getKeys().map {
            if (it.id == id) it.copy(isActive = active) else it
        }
        preferences.saveGeminiApiKeys(currentKeys)
        Timber.d("GeminiKeyManager: Toggle active ID '$id' -> $active")
    }

    suspend fun resetAllQuotaFlags() {
        val currentKeys = getKeys().map {
            it.copy(isQuotaExceeded = false)
        }
        preferences.saveGeminiApiKeys(currentKeys)
        Timber.d("GeminiKeyManager: Kuota seluruh API Key berhasil di-reset!")
    }

    suspend fun markKeyQuotaExceeded(id: String) {
        val currentKeys = getKeys().map {
            if (it.id == id) it.copy(isQuotaExceeded = true) else it
        }
        preferences.saveGeminiApiKeys(currentKeys)
        Timber.w("GeminiKeyManager: API Key ID '$id' ditandai Limit Kuota (429)!")
    }

    suspend fun getNextAvailableKey(): GeminiApiKeyItem? {
        val validKeys = getKeys().filter { it.isActive && !it.isQuotaExceeded && it.apiKey.isNotBlank() }
        if (validKeys.isEmpty()) return null

        val idx = Math.abs(roundRobinIndex.getAndIncrement()) % validKeys.size
        return validKeys[idx]
    }

    suspend fun verifyApiKey(apiKey: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key tidak boleh kosong"))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$cleanKey"

        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "ping"))
                    })
                })
            })
        }

        val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    val errorMsgStr = response.body?.string() ?: ""
                    var userError = "HTTP ${response.code}"
                    try {
                        val json = JSONObject(errorMsgStr)
                        val errObj = json.optJSONObject("error")
                        val msg = errObj?.optString("message")
                        if (!msg.isNullOrBlank()) userError = msg
                    } catch (_: Exception) {}

                    Timber.e("GeminiKeyManager: Verifikasi gagal HTTP ${response.code}: $errorMsgStr")
                    Result.failure(Exception("Gagal (HTTP ${response.code}): $userError"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "GeminiKeyManager: Gagal verifikasi API Key karena masalah jaringan")
            Result.failure(Exception("Masalah jaringan: ${e.message}"))
        }
    }
}
