package com.zaidun.photozaidun.data.mapper

import com.zaidun.photozaidun.data.local.entity.ProcessHistoryEntity
import com.zaidun.photozaidun.domain.model.ProcessHistory

fun ProcessHistoryEntity.toDomain() = ProcessHistory(
    id = id,
    folderName = folderName,
    totalImages = totalImages,
    successImages = successImages,
    failedImages = failedImages,
    duration = duration,
    uploadToDrive = uploadToDrive,
    createdAt = createdAt,
    date = createdAt,           // Tambahan
    totalPhoto = totalImages,   // Tambahan
    success = successImages,    // Tambahan
    failed = failedImages       // Tambahan
)

fun ProcessHistory.toEntity() = ProcessHistoryEntity(
    id = id,
    folderName = folderName,
    totalImages = totalImages,
    successImages = successImages,
    failedImages = failedImages,
    duration = duration,
    uploadToDrive = uploadToDrive,
    createdAt = createdAt
)
