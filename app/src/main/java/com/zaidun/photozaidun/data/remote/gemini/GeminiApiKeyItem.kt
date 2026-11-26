package com.zaidun.photozaidun.data.remote.gemini

import kotlinx.serialization.Serializable

@Serializable
data class GeminiApiKeyItem(
    val id: String,
    val label: String,
    val apiKey: String,
    val isActive: Boolean = true,
    val isQuotaExceeded: Boolean = false
)
