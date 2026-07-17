package com.zaidun.photozaidun.domain.model

data class ProcessingBatch(

    val batchNumber: Int,

    val totalBatch: Int? = null,

    val items: List<ProcessingItem>

)