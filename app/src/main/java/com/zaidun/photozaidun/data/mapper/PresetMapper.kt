package com.zaidun.photozaidun.data.mapper

import com.zaidun.photozaidun.data.local.entity.PresetEntity
import com.zaidun.photozaidun.domain.model.Preset

fun PresetEntity.toDomain() = Preset(
    id = id,
    name = name,
    resizePercent = resizePercent,
    compressionQuality = compressionQuality,
    watermarkEnabled = watermarkEnabled,
    watermarkText = watermarkText,
    uploadToDrive = uploadToDrive,
    createdAt = createdAt
)

fun Preset.toEntity() = PresetEntity(
    id = id,
    name = name,
    resizePercent = resizePercent,
    compressionQuality = compressionQuality,
    watermarkEnabled = watermarkEnabled,
    watermarkText = watermarkText,
    uploadToDrive = uploadToDrive,
    createdAt = createdAt
)