package com.zaidun.photozaidun.data.processor.compress

import com.zaidun.photozaidun.domain.model.ProcessingBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CompressionProcessor(

    private val compressor: JpegCompressor = JpegCompressor()

) {

    private val quality =
        CompressionConfig.DEFAULT_QUALITY

    fun process(

        source: Flow<ProcessingBatch>

    ): Flow<ProcessingBatch> = flow {

        source.collect { batch ->

            batch.items.forEach { item ->

                val bitmap =
                    item.bitmap ?: return@forEach

                item.jpegBytes =

                    compressor.compress(

                        bitmap,

                        quality

                    )

                // Bersihkan RAM
                bitmap.recycle()

                item.bitmap = null

            }

            emit(batch)

        }

    }

}