package com.zaidun.photozaidun.domain.repository

import android.net.Uri
import com.zaidun.photozaidun.data.source.local.scanner.ScannerResult
import kotlinx.coroutines.flow.Flow

interface ImageScannerRepository {

    fun scan(

        folderUri: Uri

    ): Flow<ScannerResult>

}