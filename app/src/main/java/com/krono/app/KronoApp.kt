package com.krono.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

// ============================================================
// KronoApp.kt
// Classe Application personalizada — ponto de inicialização
// global do app. Responsável por criar o canal de notificação
// que o MainService usará para o Foreground Service.
//
// Declarada no AndroidManifest.xml como android:name=".KronoApp"
// ============================================================

// Constantes globais acessíveis por todo o app
const val NOTIFICATION_CHANNEL_ID = "cronometro_channel"
const val NOTIFICATION_ID         = 1

// Ações dos botões da notificação — devem ser idênticas
// às declaradas no AndroidManifest.xml (NotificationActionReceiver)
const val ACTION_PLAY         = "com.krono.app.ACTION_PLAY"
const val ACTION_PAUSE        = "com.krono.app.ACTION_PAUSE"
const val ACTION_RESET        = "com.krono.app.ACTION_RESET"
const val ACTION_STOP_SERVICE = "com.krono.app.ACTION_STOP_SERVICE"
const val ACTION_SHOW_OVERLAY  = "com.krono.app.ACTION_SHOW_OVERLAY"
const val EXTRA_SHOW_DONATION  = "extra_show_donation"
const val ACTION_HIDE_OVERLAY = "com.krono.app.ACTION_HIDE_OVERLAY"
const val ACTION_START_FOCUS  = "com.krono.app.ACTION_START_FOCUS"

const val ACTION_FOCUS_DISMISSED = "com.krono.app.ACTION_FOCUS_DISMISSED"
class KronoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    // ── Canal de Notificação ─────────────────────────────────
    // Canais são obrigatórios a partir do Android 8.0 (API 26).
    // Devem ser criados antes de qualquer notificação ser exibida.
    // Recriar um canal existente não tem efeito — é seguro chamar
    // createNotificationChannel() múltiplas vezes.
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            // IMPORTANCE_LOW: exibe a notificação silenciosamente,
            // sem som nem vibração — adequado para um serviço
            // persistente que não deve incomodar o usuário.
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            // Desabilita som e vibração no nível do canal,
            // reforçando o comportamento silencioso.
            setSound(null, null)
            enableVibration(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}