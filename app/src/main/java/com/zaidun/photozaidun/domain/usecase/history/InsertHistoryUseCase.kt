package com.zaidun.photozaidun.domain.usecase.history

import com.zaidun.photozaidun.domain.model.ProcessHistory
import com.zaidun.photozaidun.domain.repository.HistoryRepository
import javax.inject.Inject

class InsertHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {

    suspend operator fun invoke(
        history: ProcessHistory
    ) {
        repository.insertHistory(history)
    }
}