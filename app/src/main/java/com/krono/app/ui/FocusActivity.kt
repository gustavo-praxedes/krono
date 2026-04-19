package com.krono.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.krono.app.ACTION_FOCUS_DISMISSED
import kotlinx.coroutines.delay

// ============================================================
// FocusActivity.kt — GIT 5
//
// Novo comportamento:
//   • Toque na área preta → oculta a sobreposição escura
//   • Após 5s sem toque → reativa a sobreposição escura
//   • Qualquer toque enquanto oculta → reseta o timer de 5s
//   • FLAG_KEEP_SCREEN_ON sempre ativo enquanto em Modo Foco
//   • onPause já NÃO encerra automaticamente (usuário pode
//     navegar e voltar; a tela preta reaparece pelo timer)
//   • Encerra apenas via ACTION_FOCUS_DISMISSED (overlay fechou
//     ou serviço parou)
// ============================================================

private const val FOCUS_HIDE_TIMEOUT_MS = 5_000L

class FocusActivity : ComponentActivity() {

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FOCUS_DISMISSED && !isFinishing) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerReceiver(
            dismissReceiver,
            IntentFilter(ACTION_FOCUS_DISMISSED),
            RECEIVER_NOT_EXPORTED
        )

        window.apply {
            // Tela ligada indefinidamente no Modo Foco,
            // independente da preferência "Manter Tela Ligada"
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor     = android.graphics.Color.BLACK
            navigationBarColor = android.graphics.Color.BLACK
        }

        setContent {
            MaterialTheme {
                FocusScreen()
            }
        }
    }

    // onPause não encerra mais — a tela preta fica "em espera"
    // e reaparece pelo timer quando o usuário voltar ao overlay.
    // Encerramento real só via ACTION_FOCUS_DISMISSED.

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(dismissReceiver) } catch (_: Exception) { }
    }
}

@Composable
private fun FocusScreen() {
    // true  = tela preta visível
    // false = tela preta oculta (usuário tocou)
    var blackVisible by remember { mutableStateOf(true) }

    // Tick para resetar o timer sem precisar desligar/ligar blackVisible
    var interactionTick by remember { mutableIntStateOf(0) }

    // Timer de reativação: 5s após o último toque
    LaunchedEffect(blackVisible, interactionTick) {
        if (!blackVisible) {
            delay(FOCUS_HIDE_TIMEOUT_MS)
            blackVisible = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camada de captura de toque — sempre presente e fillMaxSize.
        // Detecta toques mesmo quando a tela preta está oculta,
        // resetando o timer de reativação.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (blackVisible) {
                                // Primeiro toque: oculta a tela preta
                                blackVisible = false
                            } else {
                                // Toques subsequentes: apenas reset do timer
                                interactionTick++
                            }
                        }
                    )
                }
        )

        // Sobreposição preta com fade in/out suave
        AnimatedVisibility(
            visible = blackVisible,
            enter   = fadeIn(),
            exit    = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
    }
}