package com.zaidun.photozaidun.data.source.local.scanner

import android.net.Uri
import com.zaidun.photozaidun.domain.model.PhotoItem
import kotlinx.coroutines.flow.Flow

interface ImageScanner {

    fun scan(

        folderUri: Uri

    ): Flow<ScannerResult>
}