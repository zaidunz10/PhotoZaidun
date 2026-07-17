package com.zaidun.photozaidun.data.source.local.scanner

object ScannerConfig {

    /**
     * Jumlah foto yang dikirim setiap batch.
     */
    const val CHUNK_SIZE = 250

    /**
     * Maksimum subfolder yang discan.
     */
    const val MAX_DEPTH = 5

    /**
     * Format gambar yang didukung.
     */
    val SUPPORTED_EXTENSIONS = setOf(
        "jpg",
        "jpeg",
        "png",
        "webp"
    )
}