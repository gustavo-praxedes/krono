package com.gustavo.cronometro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gustavo.cronometro.ACTION_PAUSE
import com.gustavo.cronometro.ACTION_PLAY
import com.gustavo.cronometro.ACTION_RESET
import com.gustavo.cronometro.ACTION_STOP_SERVICE
import com.gustavo.cronometro.service.MainService

// ============================================================
// NotificationActionReceiver.kt
// BroadcastReceiver que intercepta os botões de ação da
// notificação persistente do Foreground Service e os repassa
// ao MainService como comandos via Intent extras.
//
// Botões da notificação → PendingIntent → este Receiver
//                                              ↓
//                                        MainService
//
// Ações tratadas (declaradas no AndroidManifest.xml):
//   • ACTION_PLAY         — inicia o cronômetro
//   • ACTION_PAUSE        — pausa o cronômetro
//   • ACTION_RESET        — reseta o cronômetro
//   • ACTION_STOP_SERVICE — encerra o serviço e fecha o overlay
//
// Por que usar um Receiver em vez de chamar o Service diretamente?
// PendingIntents de notificação não podem chamar métodos do Service
// diretamente — precisam de um Intent. Usar um Receiver como
// intermediário é o padrão recomendado pelo Android para este caso.
// ============================================================

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Monta o Intent para o MainService com o comando recebido
        val serviceIntent = Intent(context, MainService::class.java).apply {
            action = intent.action
        }

        when (intent.action) {
            ACTION_PLAY,
            ACTION_PAUSE,
            ACTION_RESET -> {
                // Repassa o comando ao MainService que está rodando.
                // startForegroundService() é usado em vez de
                // startService() para compatibilidade com Android 8+,
                // onde serviços em background não podem ser iniciados
                // diretamente. O MainService já está rodando como
                // foreground, então esta chamada apenas entrega o
                // Intent sem criar uma nova instância.
                context.startForegroundService(serviceIntent)
            }

            ACTION_STOP_SERVICE -> {
                // Encerra o MainService completamente.
                // O próprio serviço remove o overlay e a notificação
                // no seu onDestroy() — não precisamos fazer isso aqui.
                context.stopService(serviceIntent)
            }
        }
    }
}