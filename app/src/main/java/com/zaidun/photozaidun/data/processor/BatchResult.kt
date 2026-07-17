package com.zaidun.photozaidun.data.processor

import com.zaidun.photozaidun.domain.model.PhotoItem

data class BatchResult(

    val batchNumber: Int,

    val totalBatch: Int? = null,

    val photos: List<PhotoItem>

)