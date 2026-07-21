package com.zaidun.photozaidun.data.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class ExportFolderRepository @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    fun getPartFolders(
        outputFolderUri: String,
        prefix: String
    ): List<DocumentFile> {

        val folder = DocumentFile.fromTreeUri(
            context,
            Uri.parse(outputFolderUri)
        ) ?: return emptyList()

        return folder.listFiles()
            .filter {
                it.isDirectory &&
                        (it.name?.startsWith(prefix) == true)
            }
            .sortedBy { it.name }
    }

}