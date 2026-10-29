package com.routina.fit.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 達成目標時的那一聲。
 *
 * 走 STREAM_MUSIC 而不是通知音量：訓練時多半在放音樂，提示音要和音樂同一個音量
 * 才聽得到，而且手機轉靜音時還是該響。
 */
class Cues(context: Context) {

    private val appContext = context.applicationContext

    // ToneGenerator 偶爾會在音訊資源被佔用時建不起來，失敗就當作沒有聲音
    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
    } catch (t: Throwable) {
        null
    }

    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    } catch (t: Throwable) {
        null
    }

    /** 做到目標了：一聲嘟加震動 */
    fun done(sound: Boolean, vibrate: Boolean) {
        if (sound) runCatching { tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, DONE_MS) }
        if (vibrate) {
            runCatching {
                vibrator?.vibrate(VibrationEffect.createOneShot(VIBRATE_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    fun release() {
        runCatching { tone?.release() }
    }

    private companion object {
        const val VOLUME = 80
        const val DONE_MS = 450
        const val VIBRATE_MS = 250L
    }
}

