package com.zaidun.photozaidun.data.processor.bitmap

import com.zaidun.photozaidun.domain.model.ProcessingBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BitmapLoaderProcessor(

    private val loader: BitmapLoader

) {

    fun process(

        source: Flow<ProcessingBatch>

    ): Flow<ProcessingBatch> = flow {

        source.collect { batch ->

            batch.items.forEach {

                it.bitmap =

                    loader.load(

                        it.photo.uri

                    )

            }

            emit(batch)

        }

    }

}