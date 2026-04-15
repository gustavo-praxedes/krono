package com.krono.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.krono.app.ACTION_FOCUS_DISMISSED
import com.krono.app.service.MainService

// ============================================================
// FocusActivity.kt
//
// Comportamento corrigido:
//   • Toque fora do overlay → fecha APENAS a tela preta,
//     overlay continua ativo, app anterior fica visível
//   • Botão X do overlay → fecha tela preta E overlay
//     (o MainService envia ACTION_FOCUS_DISMISSED via broadcast)
//   • onPause → encerra apenas se não estiver finalizando
//   • Recebe broadcast ACTION_FOCUS_DISMISSED para auto-encerrar
//     quando o overlay é fechado pelo X
// ============================================================

class FocusActivity : ComponentActivity() {

    // Recebe sinal do MainService para encerrar junto com o overlay
    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FOCUS_DISMISSED && !isFinishing) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Registra o receiver para encerrar quando o overlay fechar
        registerReceiver(
            dismissReceiver,
            IntentFilter(ACTION_FOCUS_DISMISSED),
            RECEIVER_NOT_EXPORTED
        )

        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor     = android.graphics.Color.BLACK
            navigationBarColor = android.graphics.Color.BLACK
        }

        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                // Toque na área preta:
                                // fecha APENAS a tela preta
                                // overlay continua visível
                                // mostra o que estava aberto antes
                                onTap = { finish() }
                            )
                        }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Encerra se saiu por outros meios (Home, etc.)
        // mas NÃO notifica o service — overlay continua ativo
        if (!isFinishing) finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(dismissReceiver) } catch (_: Exception) { }
    }
}