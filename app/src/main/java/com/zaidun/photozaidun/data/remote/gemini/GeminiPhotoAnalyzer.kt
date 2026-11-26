package com.zaidun.photozaidun.data.remote.gemini

import android.content.Context
import android.graphics.*
import android.util.Base64
import com.zaidun.photozaidun.data.local.datastore.UserPreferencesDataStore
import com.zaidun.photozaidun.domain.model.PhotoAiMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiPhotoAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyManager: GeminiKeyManager,
    private val preferences: UserPreferencesDataStore
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val defaultBikeModels = listOf(
        "Beat", "Vario", "Scoopy", "PCX", "ADV",
        "Nmax", "Aerox", "Mio", "Vespa Matic", "Xmax",
        "Forza", "R15", "Ninja 150", "CBR150", "CBR 250",
        "ZX25R", "Satria FU", "Lainnya"
    )

    private val bikeModelsList: List<String> by lazy {
        try {
            context.assets.open("model-motor/labels.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }
                    .filter { it.isNotBlank() && !it.startsWith("#") }
                    .map { it.replace(Regex("^\\d+\\s*"), "").trim() }
                    .distinct()
                    .toList()
            }.ifEmpty { defaultBikeModels }
        } catch (e: Exception) {
            Timber.e(e, "Gagal membaca labels.txt dari assets")
            defaultBikeModels
        }
    }

    suspend fun analyzeBatch(bitmaps: List<Bitmap>): List<PhotoAiMetadata?> = withContext(Dispatchers.IO) {
        if (bitmaps.isEmpty()) return@withContext emptyList()
        val scanStart = System.currentTimeMillis()

        val results = MutableList<PhotoAiMetadata?>(bitmaps.size) { null }
        val initialKey = keyManager.getNextAvailableKey()
        if (initialKey == null) {
            Timber.tag("AI_SCAN").w("Gemini: Tidak ada API Key yang aktif & memiliki kuota. AI Pre-Processing dilewati.")
            return@withContext results
        }

        // 1. Buat Kolase 2x2
        val collageBitmap = create2x2Collage(bitmaps)
        val base64Image = bitmapToBase64Jpeg(collageBitmap, quality = 80)
        if (!collageBitmap.isRecycled) collageBitmap.recycle()

        val modelName = preferences.selectedGeminiModel.first().ifBlank { "gemini-3.5-flash-lite" }.replace(',', '.')
        val promptText = """
            You are an expert AI vehicle and license plate analyst for Indonesian automotive events. 
            Analyze this 2x2 collage of vehicle photos labeled 1, 2, 3, 4 in top-left corners.
            Achieve maximum precision (>90% accuracy) by examining body shape, headlamps, badges, and text carefully.
            
            For each photo present (1 to ${bitmaps.size}), extract:
            1. plate_number: Read Indonesian vehicle license plate characters with high precision. Format strictly as [Region Code][Space][Numbers][Space][Series Letters] (e.g. 'D 6420 ZDO', 'B 1234 XYZ'). If digits or letters are blurry or unreadable, return null. Do NOT guess.
            2. bike_model: Identify the correct vehicle model name as a simple, general parent name (e.g. 'Beat', 'Vario', 'Scoopy', 'PCX', 'ADV', 'Nmax', 'Aerox', 'Xmax', 'Mio', 'Vespa', 'Ninja', 'CBR', 'R15', 'Satria', 'Xenia', 'Avanza', 'Harley-Davidson'). Do NOT include sub-trims (e.g. no 'Beat Street' -> use 'Beat').
            3. bike_color: Dominant vehicle body color chosen strictly from: [Hitam, Putih, Abu-abu, Merah, Biru, Hijau, Kuning, Oranye, Ungu, Pink].
            4. jacket_color: Dominant color of the rider's jacket chosen strictly from: [Hitam, Putih, Abu-abu, Merah, Biru, Hijau, Kuning, Oranye, Ungu, Pink] or null if rider is absent.
        """.trimIndent()

        val payloadJson = buildRequestPayload(base64Image, promptText)
        val body = payloadJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        var currentKey: GeminiApiKeyItem? = initialKey
        var success = false
        var retryAttempts = 0

        while (currentKey != null && !success && retryAttempts < 6) {
            val apiKey = currentKey.apiKey
            val keyId = currentKey.id
            val keyLabel = currentKey.label
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: ""

                    if (responseCode == 429) {
                        Timber.tag("AI_SCAN").w("Gemini API Key '$keyLabel' Limit Kuota (429)! Rotasi ke Key berikutnya...")
                        keyManager.markKeyQuotaExceeded(keyId)
                        currentKey = keyManager.getNextAvailableKey()
                        retryAttempts++
                    } else if (responseCode == 503) {
                        Timber.tag("AI_SCAN").w("Gemini API Overload (503) pada Key '$keyLabel'! Rotasi ke Key berikutnya...")
                        delay(500)
                        currentKey = keyManager.getNextAvailableKey() ?: currentKey
                        retryAttempts++
                    } else if (response.isSuccessful) {
                        val parsedResults = parseGeminiResponse(responseBody, bitmaps.size)
                        for (i in parsedResults.indices) {
                            if (i < results.size) {
                                results[i] = parsedResults[i]
                            }
                        }
                        success = true
                        val scanDuration = System.currentTimeMillis() - scanStart
                        Timber.tag("AI_SCAN").i("⚡ Gemini AI Scan 2x2 (${bitmaps.size} foto) SELESAI dalam ${scanDuration}ms ($keyLabel, Model=$modelName)")
                    } else {
                        Timber.tag("AI_SCAN").e("Gemini API Error HTTP $responseCode: $responseBody. Rotasi Key...")
                        currentKey = keyManager.getNextAvailableKey()
                        retryAttempts++
                    }
                }
            } catch (e: Exception) {
                Timber.tag("AI_SCAN").e(e, "Gemini API Network Exception. Rotasi Key...")
                currentKey = keyManager.getNextAvailableKey()
                retryAttempts++
            }
        }

        results
    }

    private fun create2x2Collage(bitmaps: List<Bitmap>): Bitmap {
        val collageWidth = 1024
        val collageHeight = 1024
        val cellWidth = 512
        val cellHeight = 512

        val collage = Bitmap.createBitmap(collageWidth, collageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(collage)
        canvas.drawColor(Color.BLACK)

        val badgeBgPaint = Paint().apply {
            color = Color.parseColor("#E6EF4444") // Red background badge
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 36f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        for (i in bitmaps.indices) {
            val src = bitmaps[i]
            val col = i % 2
            val row = i / 2
            val left = col * cellWidth
            val top = row * cellHeight

            val destRect = Rect(left, top, left + cellWidth, top + cellHeight)
            canvas.drawBitmap(src, null, destRect, null)

            // Gambar Badge Nomor 1..4
            val badgeRadius = 28f
            val badgeCenterX = left + 45f
            val badgeCenterY = top + 45f

            canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, badgeBgPaint)
            val textY = badgeCenterY - ((badgeTextPaint.descent() + badgeTextPaint.ascent()) / 2)
            canvas.drawText("${i + 1}", badgeCenterX, textY, badgeTextPaint)
        }

        return collage
    }

    private fun bitmapToBase64Jpeg(bitmap: Bitmap, quality: Int): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun buildRequestPayload(base64Image: String, promptText: String): JSONObject {
        val enumColors = JSONArray(listOf(
            "Hitam", "Putih", "Abu-abu", "Merah", "Biru", "Hijau", "Kuning", "Oranye", "Ungu", "Pink"
        ))

        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        put(JSONObject().put("text", promptText))
                    })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
                put("responseSchema", JSONObject().apply {
                    put("type", "ARRAY")
                    put("items", JSONObject().apply {
                        put("type", "OBJECT")
                        put("properties", JSONObject().apply {
                            put("index", JSONObject().put("type", "INTEGER"))
                            put("plate_number", JSONObject().apply {
                                put("type", "STRING")
                                put("nullable", true)
                            })
                            put("bike_model", JSONObject().apply {
                                put("type", "STRING")
                                put("description", "Specific motorcycle model name, e.g. Honda Beat, Yamaha Aerox, BMW S1000RR, Ducati Panigale, etc.")
                            })
                            put("bike_color", JSONObject().apply {
                                put("type", "STRING")
                                put("enum", enumColors)
                            })
                            put("jacket_color", JSONObject().apply {
                                put("type", "STRING")
                                put("nullable", true)
                                put("enum", enumColors)
                            })
                        })
                        put("required", JSONArray(listOf("index", "bike_model", "bike_color")))
                    })
                })
            })
        }
    }

    private fun parseGeminiResponse(jsonString: String, batchSize: Int): List<PhotoAiMetadata?> {
        val resultList = MutableList<PhotoAiMetadata?>(batchSize) { null }

        try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates") ?: return resultList
            if (candidates.length() == 0) return resultList

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return resultList
            val parts = content.optJSONArray("parts") ?: return resultList
            if (parts.length() == 0) return resultList

            val jsonText = parts.getJSONObject(0).optString("text", "")
            if (jsonText.isBlank()) return resultList

            val jsonArray = JSONArray(jsonText)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val idx = item.optInt("index", i + 1) - 1

                if (idx in 0 until batchSize) {
                    val plate = item.optString("plate_number", "").takeIf { it.isNotBlank() && it != "null" }
                    val model = item.optString("bike_model", "").takeIf { it.isNotBlank() && it != "null" }
                    val bikeColor = item.optString("bike_color", "").takeIf { it.isNotBlank() && it != "null" }
                    val jacketColor = item.optString("jacket_color", "").takeIf { it.isNotBlank() && it != "null" }

                    resultList[idx] = PhotoAiMetadata(
                        bikeModel = model,
                        plateNumber = plate,
                        bikeColor = bikeColor,
                        jacketColor = jacketColor
                    )
                }
            }
        } catch (e: Exception) {
            Timber.tag("AI_SCAN").e(e, "Gagal memparsing JSON respon Gemini: $jsonString")
        }

        return resultList
    }
}
