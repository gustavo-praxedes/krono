package com.krono.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krono.app.data.TimerPreferences
import com.krono.app.service.MainService

// ============================================================
// BootReceiver.kt
// Correção: removido putExtra(EXTRA_LAUNCHED_FROM_BOOT)
// — o extra nunca era lido pelo MainService
// ============================================================

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
        if (intent.action !in validActions) return

        val timerPrefs = TimerPreferences(context)

        if (!timerPrefs.isServiceActive()) return

        val state = timerPrefs.loadState()
        if (state.startTime == -1L && state.pauseOffset == 0L && !state.isRunning) {
            timerPrefs.clearState()
            return
        }

        context.startForegroundService(
            Intent(context, MainService::class.java)
        )
    }
}