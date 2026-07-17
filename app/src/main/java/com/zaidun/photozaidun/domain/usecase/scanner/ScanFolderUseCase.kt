package com.zaidun.photozaidun.domain.usecase.scanner

import android.net.Uri
import com.zaidun.photozaidun.domain.repository.ImageScannerRepository
import javax.inject.Inject

class ScanFolderUseCase @Inject constructor(

    private val repository: ImageScannerRepository

) {

    operator fun invoke(

        folderUri: Uri

    ) = repository.scan(folderUri)

}