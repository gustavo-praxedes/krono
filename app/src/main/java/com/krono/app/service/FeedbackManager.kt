package com.krono.app.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.krono.app.data.OverlayConfig

class FeedbackManager(context: Context) {

    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_ALARM, 80)
    } catch (_: Exception) {
        null
    }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager)
            .defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun triggerFeedback(config: OverlayConfig) {
        if (config.isBeepEnabled) {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
        if (config.isVibrationEnabled) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}