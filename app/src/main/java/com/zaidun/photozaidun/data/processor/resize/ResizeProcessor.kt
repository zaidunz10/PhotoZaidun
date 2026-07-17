package com.zaidun.photozaidun.data.processor.resize

import com.zaidun.photozaidun.data.processor.BatchResult
import com.zaidun.photozaidun.domain.model.ProcessingBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ResizeProcessor(

    private val imageResizer: ImageResizer = ImageResizer(),
    private val percent: Int = ResizeConfig.DEFAULT_PERCENT

) {

    fun process(

        source: Flow<ProcessingBatch>

    ): Flow<ProcessingBatch> = flow {

        source.collect { batch ->

            /**
             * STEP 12
             * Resize semua foto dalam batch.
             */
            batch.items.forEach {

                val bitmap =

                    it.bitmap ?: return@forEach

                it.bitmap =

                    imageResizer.resize(

                        bitmap,

                        percent

                    )

            }

            emit(batch)

        }

    }

}