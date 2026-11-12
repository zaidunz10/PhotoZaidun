package com.zaidun.carifoto.data.remote

import com.zaidun.carifoto.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// DTO untuk User / Fotografer di Supabase (`public.users`)
@Serializable
data class CariFotoUserDto(
    val id: String = "",
    val name: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

// DTO untuk Sesi Hunting Fotografer di Supabase (`public.sessions`)
@Serializable
data class CariFotoSessionDto(
    val id: Long = 0,
    @SerialName("photographer_id") val photographerId: String? = null,
    val location: String = "",
    @SerialName("session_date") val sessionDate: String? = null,
    @SerialName("whatsapp_number") val whatsappNumber: String? = null,
    @SerialName("instagram_link") val instagramLink: String? = null,
    @SerialName("price_single") val priceSingle: Int? = 10000,
    @SerialName("drive_folder_id") val driveFolderId: String? = null,
    @SerialName("is_published") val isPublished: Boolean? = true
)

// DTO untuk Foto Individu di Supabase (`public.photos`)
@Serializable
data class CariFotoPhotoDto(
    val id: Long = 0,
    @SerialName("session_id") val sessionId: Long = 0,
    @SerialName("drive_file_id") val driveFileId: String = "",
    @SerialName("file_name") val fileName: String = "",
    @SerialName("exif_time") val exifTime: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// DTO untuk Transaksi Order di Supabase (`public.orders`)
@Serializable
data class CariFotoOrderDto(
    val id: Long? = null,
    @SerialName("rider_id") val riderId: String = "",
    @SerialName("photo_id") val photoId: Long = 0,
    val price: Int = 10000,
    @SerialName("admin_fee") val adminFee: Int = 2000,
    @SerialName("total_price") val totalPrice: Int = 12000,
    val status: String = "PAID"
)

object CariFotoSupabaseHelper {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(json)
            install(Postgrest)
        }
    }
}
