package com.zaidun.photozaidun.utils

import android.content.Context
import android.media.MediaPlayer
import timber.log.Timber

object SoundPlayer {
    fun playAssetSound(context: Context, fileName: String) {
        try {
            val assetPath = if (fileName.startsWith("sound/")) fileName else "sound/$fileName"
            val afd = context.assets.openFd(assetPath)
            val player = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                prepare()
                start()
                setOnCompletionListener { it.release() }
            }
        } catch (e: Exception) {
            Timber.e(e, "Gagal memutar mp3 $fileName dari assets")
        }
    }
}
