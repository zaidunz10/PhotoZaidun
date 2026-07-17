package com.zaidun.photozaidun.data.source.local.scanner

import com.zaidun.photozaidun.domain.model.PhotoItem

sealed interface ScannerResult {

    data class Progress(

        val scanned: Int,

        val batch: List<PhotoItem>

    ) : ScannerResult

    data object Finished : ScannerResult

    data class Error(

        val message: String

    ) : ScannerResult
}