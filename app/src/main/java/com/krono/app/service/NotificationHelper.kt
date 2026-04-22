package com.krono.app.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.krono.app.*
import com.krono.app.data.TimerState
import com.krono.app.data.toFormattedTime
import com.krono.app.receiver.NotificationActionReceiver
import com.krono.app.ui.MainActivity

class NotificationHelper(private val context: Context) {

    private val contentPendingIntent: PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun buildNotification(
        timerState: TimerState,
        showHours: Boolean,
        showSeconds: Boolean
    ): Notification {

        fun actionIntent(action: String, requestCode: Int): PendingIntent {
            val i = Intent(context, NotificationActionReceiver::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context, requestCode, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .addAction(
                if (timerState.isRunning) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                if (timerState.isRunning) context.getString(R.string.action_pause)
                else context.getString(R.string.action_play),
                if (timerState.isRunning) actionIntent(ACTION_PAUSE, 1)
                else actionIntent(ACTION_PLAY, 2)
            )
            .addAction(
                android.R.drawable.ic_menu_revert,
                context.getString(R.string.action_reset),
                actionIntent(ACTION_RESET, 3)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.action_stop_service),
                actionIntent(ACTION_STOP_SERVICE, 4)
            )

        if (timerState.isRunning && timerState.startTime != -1L) {
            val elapsedSinceStart = System.currentTimeMillis() - timerState.startTime
            val totalElapsed      = timerState.pauseOffset + elapsedSinceStart
            val whenMs            = System.currentTimeMillis() - totalElapsed

            builder
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
                .setWhen(whenMs)
                .setShowWhen(true)
                .setContentText(context.getString(R.string.notification_text_running))

        } else {
            val frozenTime = timerState.elapsedMs.toFormattedTime(
                showHours   = showHours,
                showSeconds = showSeconds
            )
            builder
                .setUsesChronometer(false)
                .setShowWhen(false)
                .setContentText(frozenTime)
        }

        return builder.build()
    }
}