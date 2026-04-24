package com.krono.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.krono.app.viewmodel.TimerViewModel

// Constantes globais do App
const val NOTIFICATION_ID         = 1
const val CHANNEL_ID              = "timer_channel"
const val ACTION_PLAY             = "com.krono.app.ACTION_PLAY"
const val ACTION_PAUSE            = "com.krono.app.ACTION_PAUSE"
const val ACTION_RESET            = "com.krono.app.ACTION_RESET"
const val ACTION_STOP_SERVICE     = "com.krono.app.ACTION_STOP_SERVICE"
const val ACTION_SHOW_OVERLAY     = "com.krono.app.ACTION_SHOW_OVERLAY"
const val ACTION_HIDE_OVERLAY     = "com.krono.app.ACTION_HIDE_OVERLAY"
const val ACTION_START_FOCUS      = "com.krono.app.ACTION_START_FOCUS"
const val EXTRA_SHOW_DONATION     = "extra_show_donation"

class KronoApp : Application() {

    // Singleton do ViewModel passando a referência da application
    val timerViewModel: TimerViewModel by lazy {
        TimerViewModel(this)
    }

    override fun onCreate() {
        super.onCreate()
        
        // 1. Inicializa o Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Cria o canal de notificação
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name        = "Cronômetro Flutuante"
            val description = "Exibe o tempo e controles na barra de notificações"
            val importance  = NotificationManager.IMPORTANCE_LOW
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
                setShowBadge(false)
            }
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
