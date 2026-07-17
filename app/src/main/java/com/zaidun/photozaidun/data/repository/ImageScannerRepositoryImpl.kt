package com.zaidun.photozaidun.data.repository

import android.net.Uri
import com.zaidun.photozaidun.data.source.local.scanner.ImageScanner
import com.zaidun.photozaidun.data.source.local.scanner.ScannerResult
import com.zaidun.photozaidun.domain.repository.ImageScannerRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ImageScannerRepositoryImpl @Inject constructor(

    private val scanner: ImageScanner

) : ImageScannerRepository {

    override fun scan(

        folderUri: Uri

    ): Flow<ScannerResult> {

        return scanner.scan(folderUri)

    }

}