package com.zaidun.photozaidun.data.processor.watermark

import android.graphics.Bitmap
import com.zaidun.photozaidun.domain.model.ProcessingBatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WatermarkProcessor(
    private val drawer: WatermarkDrawer,
    private val watermarkBitmap: Bitmap?,
    private val opacity: Float,
    private val scale: Float,
    private val logoOffsetX: Float,
    private val logoOffsetY: Float,
    private val infoOffsetX: Float,
    private val infoOffsetY: Float,
    private val infoSize: Float,
    private val showFilename: Boolean,
    private val showPart: Boolean
) {

    fun process(
        source: Flow<ProcessingBatch>
    ): Flow<ProcessingBatch> = flow {
        source.collect { batch ->
            batch.items.forEach { item ->
                val bitmap = item.bitmap ?: return@forEach

                item.bitmap = drawer.draw(
                    bitmap = bitmap,
                    watermark = watermarkBitmap,
                    alpha = opacity,
                    scale = scale,
                    logoOffsetX = logoOffsetX,
                    logoOffsetY = logoOffsetY,
                    showFilename = showFilename,
                    showPart = showPart,
                    fileName = item.photo.name,
                    partName = "Part ${batch.batchNumber}",
                    infoOffsetX = infoOffsetX,
                    infoOffsetY = infoOffsetY,
                    infoSize = infoSize
                )
            }
            emit(batch)
        }
    }
}
