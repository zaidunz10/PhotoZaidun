package com.zaidun.photozaidun.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.zaidun.photozaidun.BuildConfig

@Serializable
data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

// DTO untuk Tabel `sessions`
@Serializable
data class SessionInsertDto(
    @SerialName("photographer_id") val photographerId: String,
    val location: String,
    @SerialName("whatsapp_number") val whatsappNumber: String,
    @SerialName("instagram_link") val instagramLink: String,
    @SerialName("price_single") val priceSingle: Int = 0,
    @SerialName("is_published") val isPublished: Boolean = true
)

// DTO untuk Respon Insert Tabel `sessions`
@Serializable
data class SessionResponseDto(
    val id: Long
)

// DTO untuk Update Link Drive ke Tabel `sessions`
@Serializable
data class SessionUpdateDriveDto(
    @SerialName("drive_folder_id") val driveFolderId: String,
    @SerialName("is_published") val isPublished: Boolean = true
)

// DTO untuk Mengambil Daftar Lokasi Unik dari Tabel `sessions`
@Serializable
data class SessionLocationDto(
    val location: String
)

object SupabaseHelper {
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Postgrest)
        }
    }
}
