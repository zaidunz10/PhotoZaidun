package com.zaidun.photozaidun.domain.model

data class WatermarkTemplate(

    val id: Long = 0,

    val templateName: String,

    val text: String,

    val opacity: Float,

    val textSize: Float,

    val position: String
)