package com.gustavo.cronometro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gustavo.cronometro.ACTION_PAUSE
import com.gustavo.cronometro.ACTION_PLAY
import com.gustavo.cronometro.ACTION_RESET
import com.gustavo.cronometro.ACTION_STOP_SERVICE
import com.gustavo.cronometro.service.MainService

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, MainService::class.java).apply {
            action = intent.action
        }

        when (intent.action) {
            ACTION_PLAY,
            ACTION_PAUSE,
            ACTION_RESET,
            ACTION_STOP_SERVICE -> {
                context.startForegroundService(serviceIntent)
            }
        }
    }
}