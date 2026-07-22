package com.zaidun.photozaidun.utils

import kotlin.math.ln
import kotlin.math.pow

fun Long.toReadableSize(): String {

    if (this <= 0L) return "0 B"

    val units = listOf("B", "KB", "MB", "GB", "TB")

    val digit = (ln(toDouble()) / ln(1024.0)).toInt()

    val size = this / 1024.0.pow(digit)

    return "%.1f %s".format(size, units[digit])

}