package com.zaidun.photozaidun.data.source.local.scanner

import androidx.documentfile.provider.DocumentFile

object ScannerUtil {

    fun isSupportedImage(
        file: DocumentFile
    ): Boolean {

        if (!file.isFile) return false

        val extension =
            file.name
                ?.substringAfterLast('.', "")
                ?.lowercase()
                ?: return false

        return extension in
                ScannerConfig.SUPPORTED_EXTENSIONS
    }

}