package com.zaidun.photozaidun.data.processor

import com.zaidun.photozaidun.data.source.local.scanner.ScannerResult
import com.zaidun.photozaidun.domain.model.PhotoItem
import com.zaidun.photozaidun.domain.model.ProcessingBatch
import com.zaidun.photozaidun.domain.model.ProcessingItem
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BatchProcessor @Inject constructor() {

    private val batchSize = BatchConfig.DEFAULT_BATCH_SIZE

    fun process(
        source: Flow<ScannerResult>
    ): Flow<ProcessingBatch> = flow {
        val buffer = ArrayList<PhotoItem>(batchSize)
        var batchNumber = 1

        source.collect { result ->
            if (result is ScannerResult.Progress) {
                result.batch.forEach { photo ->
                    buffer.add(photo)
                    if (buffer.size >= batchSize) {
                        emit(
                            ProcessingBatch(
                                batchNumber = batchNumber,
                                items = buffer.map { ProcessingItem(it) }
                            )
                        )
                        batchNumber++
                        buffer.clear()
                    }
                }
            }
        }

        if (buffer.isNotEmpty()) {
            emit(
                ProcessingBatch(
                    batchNumber = batchNumber,
                    items = buffer.map { ProcessingItem(it) }
                )
            )
        }
    }
}