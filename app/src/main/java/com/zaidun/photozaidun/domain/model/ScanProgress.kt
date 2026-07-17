package com.zaidun.photozaidun.domain.model

data class ScanProgress(

    val scanned: Int,

    val total: Int? = null,

    val finished: Boolean = false

)