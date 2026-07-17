package com.zaidun.photozaidun.domain.usecase

import android.net.Uri
import com.zaidun.photozaidun.data.processor.BatchProcessor
import com.zaidun.photozaidun.data.processor.bitmap.BitmapLoaderProcessor
import com.zaidun.photozaidun.data.processor.compress.CompressionProcessor
import com.zaidun.photozaidun.data.processor.export.ExportProcessor
import com.zaidun.photozaidun.data.processor.resize.ResizeProcessor
import com.zaidun.photozaidun.data.processor.watermark.WatermarkProcessor
import com.zaidun.photozaidun.domain.model.ProcessingBatch
import com.zaidun.photozaidun.domain.usecase.history.InsertHistoryUseCase
import com.zaidun.photozaidun.domain.usecase.scanner.ScanFolderUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProcessPhotosUseCase @Inject constructor(

    private val scanFolderUseCase: ScanFolderUseCase,

    private val batchProcessor: BatchProcessor,

    private val bitmapLoaderProcessor: BitmapLoaderProcessor,

    private val resizeProcessor: ResizeProcessor,

    private val watermarkProcessor: WatermarkProcessor,

    private val compressionProcessor: CompressionProcessor,

    private val exportProcessor: ExportProcessor,

    private val insertHistoryUseCase: InsertHistoryUseCase

) {

    fun execute(

        outputFolder: Uri

    ): Flow<ProcessingBatch> {

        return exportProcessor.process(

            outputFolder,

            compressionProcessor.process(

                watermarkProcessor.process(

                    resizeProcessor.process(

                        bitmapLoaderProcessor.process(

                            batchProcessor.process(

                                scanFolderUseCase(outputFolder)

                            )

                        )

                    )

                )

            )

        )

    }

}