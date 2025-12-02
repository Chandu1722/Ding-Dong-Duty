package com.example.alarmpuzzle

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Vibrator

object AlarmSoundManager {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun playSound(context: Context, ringtoneUri: String?) {
        stopSound()
        val uri = ringtoneUri?.let { Uri.parse(it) } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        player = MediaPlayer.create(context, uri)?.apply {
            isLooping = true
            start()
        }
    }

    fun setVibrator(v: Vibrator) {
        vibrator = v
    }

    fun stopSound() {
        try {
            player?.stop()
            player?.release()
            vibrator?.cancel()
        } finally {
            player = null
            vibrator = null
        }
    }
}

