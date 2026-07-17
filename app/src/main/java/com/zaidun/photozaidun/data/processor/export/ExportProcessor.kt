package com.zaidun.photozaidun.data.processor.export

import android.net.Uri
import com.zaidun.photozaidun.domain.model.ProcessingBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ExportProcessor(

    private val exporter: ImageExporter

) {

    fun process(

        folderUri: Uri,

        source: Flow<ProcessingBatch>

    ): Flow<ProcessingBatch> = flow {

        source.collect { batch ->

            batch.items.forEach { item ->

                val jpeg = item.jpegBytes ?: return@forEach

                item.outputUri = exporter.export(
                    folderUri,
                    item.photo.name,
                    jpeg
                )

                item.jpegBytes = null

            }

            emit(batch)

        }

    }

}