package com.zaidun.photozaidun.data.repository

import com.zaidun.photozaidun.data.local.dao.ProcessHistoryDao
import com.zaidun.photozaidun.data.mapper.toDomain
import com.zaidun.photozaidun.data.mapper.toEntity
import com.zaidun.photozaidun.domain.model.ProcessHistory
import com.zaidun.photozaidun.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val dao: ProcessHistoryDao
) : HistoryRepository {

    override fun getHistory(): Flow<List<ProcessHistory>> {
        return dao.getAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertHistory(history: ProcessHistory) {
        dao.insert(history.toEntity())
    }
}