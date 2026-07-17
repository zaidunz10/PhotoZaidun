package com.zaidun.photozaidun.domain.usecase.history

import com.zaidun.photozaidun.domain.repository.HistoryRepository
import javax.inject.Inject

class GetHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {

    operator fun invoke() =
        repository.getHistory()
}