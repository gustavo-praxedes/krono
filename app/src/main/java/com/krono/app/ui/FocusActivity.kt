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
//   • Após 6s sem toque → reativa a sobreposição escura
//   • Qualquer toque enquanto oculta → reseta o timer de 10s
//   • FLAG_KEEP_SCREEN_ON sempre ativo enquanto em Modo Foco
//   • onPause já NÃO encerra automaticamente (usuário pode
//     navegar e voltar; a tela preta reaparece pelo timer)
//   • Encerra apenas via ACTION_FOCUS_DISMISSED (overlay fechou
//     ou serviço parou)
// ============================================================

private const val FOCUS_HIDE_TIMEOUT_MS = 6_000L

class FocusActivity : ComponentActivity() {

    // Tick incrementável capturado no nível da Window (Activity) 
    // para podermos atualizar a reativação mesmo com layout pass-through
    val globalInteractionTick = mutableIntStateOf(0)

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_OUTSIDE) {
            globalInteractionTick.intValue++
        }
        return super.dispatchTouchEvent(ev)
    }

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
                FocusScreen(this)
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
private fun FocusScreen(activity: FocusActivity) {
    // true  = tela preta visível
    // false = tela preta oculta (usuário tocou)
    var blackVisible by remember { mutableStateOf(true) }

    // Timer de reativação: 10s após o último toque
    LaunchedEffect(blackVisible, activity.globalInteractionTick.intValue) {
        if (!blackVisible) {
            delay(FOCUS_HIDE_TIMEOUT_MS)
            blackVisible = true
        }
    }

    LaunchedEffect(blackVisible) {
        val window = activity.window
        if (blackVisible) {
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        } else {
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
            // Encolher a janela força o Android a disparar ACTION_OUTSIDE para toques 
            // no resto (100%) da tela
            window.setLayout(1, 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

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
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                blackVisible = false
                            }
                        )
                    }
            )
        }
    }
}