
package com.zaidun.carifoto.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DrivePhotoItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val thumbnailLink: String? = null,
    val webViewLink: String? = null,
    val imageUrl: String
)
