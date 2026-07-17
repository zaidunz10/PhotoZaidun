package com.zaidun.photozaidun.data.processor.watermark

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.snapping.SnapPosition.Center.position
import com.google.common.math.Quantiles.scale
import com.zaidun.photozaidun.domain.model.ProcessingBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WatermarkProcessor(


    private val drawer: WatermarkDrawer,

    private val watermarkBitmap: Bitmap,

    private val opacity: Float,

    private val scale: Float,

    private val position: String
) {

    fun process(

        source: Flow<ProcessingBatch>

    ): Flow<ProcessingBatch> = flow {

        source.collect { batch ->

            batch.items.forEach { item ->

                val bitmap =
                    item.bitmap ?: return@forEach

                item.bitmap =

                    drawer.draw(
                        bitmap,
                        watermarkBitmap,
                        opacity,
                        scale,
                        position
                    )

            }

            emit(batch)

        }

    }

}