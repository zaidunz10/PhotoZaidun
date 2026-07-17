package com.zaidun.photozaidun.data.source.local.scanner

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SafImageScanner @Inject constructor(

    @ApplicationContext
    private val context: Context

) : ImageScanner {

    private val scanner by lazy {

        CursorScanner(
            context.contentResolver
        )

    }

    override fun scan(folderUri: Uri): Flow<ScannerResult> {
        return scanner.scan(folderUri).map { photo ->
            ScannerResult.Progress(
                scanned = 1,
                batch = listOf(photo)
            )
        }
    }

}