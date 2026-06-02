package com.aura.gridgage

import android.content.Context
import android.media.MediaPlayer

/* Manages background music and sounds */
object SoundManager {
    private var mediaPlayer: MediaPlayer? = null
    private var isMuted = false

    fun init(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.bg_music)
            mediaPlayer?.isLooping = true
        }
    }

    fun startMusic() {
        if (!isMuted) {
            mediaPlayer?.start()
        }
    }

    fun stopMusic() {
        mediaPlayer?.pause()
    }

    fun toggleMute(context: Context): Boolean {
        isMuted = !isMuted
        if (isMuted) {
            mediaPlayer?.pause()
        } else {
            if (mediaPlayer == null) init(context)
            mediaPlayer?.start()
        }
        return isMuted
    }

    fun isMuted() = isMuted
}