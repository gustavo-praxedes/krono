package com.krono.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.krono.app.viewmodel.TimerViewModel

const val NOTIFICATION_CHANNEL_ID = "krono_channel"
const val NOTIFICATION_ID         = 1
const val ACTION_PLAY             = "com.krono.app.ACTION_PLAY"
const val ACTION_PAUSE            = "com.krono.app.ACTION_PAUSE"
const val ACTION_RESET            = "com.krono.app.ACTION_RESET"
const val ACTION_STOP_SERVICE     = "com.krono.app.ACTION_STOP_SERVICE"
const val ACTION_SHOW_OVERLAY     = "com.krono.app.ACTION_SHOW_OVERLAY"
const val ACTION_HIDE_OVERLAY     = "com.krono.app.ACTION_HIDE_OVERLAY"
const val ACTION_START_FOCUS      = "com.krono.app.ACTION_START_FOCUS"
const val ACTION_FOCUS_DISMISSED  = "com.krono.app.ACTION_FOCUS_DISMISSED"
const val EXTRA_SHOW_DONATION     = "extra_show_donation"

class KronoApp : Application() {
    // ── Singleton do ViewModel ────────────────────────────────
    // Único ponto de verdade do cronômetro.
    // Compartilhado entre MainActivity, MainService e qualquer
    // outro componente que precise do estado do timer.
    val timerViewModel: TimerViewModel by lazy {
        TimerViewModel(this)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Krono",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Cronômetro ativo"
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}