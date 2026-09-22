package com.zaidun.photozaidun.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.zaidun.photozaidun.R

class SoundManager(context: Context) {
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(audioAttributes)
        .build()

    private val soundMap = mutableMapOf<String, Int>()

    init {
        // Load suara ke memori (pastikan file ada di res/raw)
        // soundMap["click"] = soundPool.load(context, R.raw.click, 1)
        // soundMap["success"] = soundPool.load(context, R.raw.success, 1)
        // soundMap["error"] = soundPool.load(context, R.raw.error, 1)
    }

    fun playSound(name: String) {
        soundMap[name]?.let { id ->
            soundPool.play(id, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
