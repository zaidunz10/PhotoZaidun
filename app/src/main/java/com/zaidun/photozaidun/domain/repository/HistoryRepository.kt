package com.zaidun.photozaidun.domain.repository

import com.zaidun.photozaidun.domain.model.ProcessHistory
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {

    fun getHistory(): Flow<List<ProcessHistory>>

    suspend fun insertHistory(
        history: ProcessHistory
    )
}