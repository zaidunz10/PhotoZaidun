package com.zaidun.photozaidun.domain.model

data class Preset(

    val id: Long = 0,

    val name: String,

    val resizePercent: Int,

    val compressionQuality: Int,

    val watermarkEnabled: Boolean,

    val watermarkText: String,

    val uploadToDrive: Boolean,

    val createdAt: Long
)