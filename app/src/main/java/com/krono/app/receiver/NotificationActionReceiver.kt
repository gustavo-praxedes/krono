package com.krono.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.krono.app.ACTION_PAUSE
import com.krono.app.ACTION_PLAY
import com.krono.app.ACTION_RESET
import com.krono.app.ACTION_SHOW_OVERLAY
import com.krono.app.ACTION_STOP_SERVICE
import com.krono.app.service.MainService
import com.krono.app.ACTION_HIDE_OVERLAY

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
            ACTION_SHOW_OVERLAY -> {
                context.startForegroundService(serviceIntent)
            }
            ACTION_HIDE_OVERLAY -> {
                context.startForegroundService(serviceIntent)
            }
        }
    }
}