package com.krono.app.ui

import android.content.Intent
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
// Tela preta fullscreen para o Modo Foco.
//
// Correções:
//   • Ao encerrar (onPause ou toque), notifica o MainService
//     via ACTION_FOCUS_DISMISSED para que overlayVisible seja
//     atualizado e o overlay possa ser reexibido corretamente.
//   • onPause encerra a Activity apenas se não estiver
//     finalizando já (evita duplo dispatch).
// ============================================================

class FocusActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                                onTap = { dismissFocus() }
                            )
                        }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Encerra se o usuário saiu por outros meios (Home, notificação)
        if (!isFinishing) {
            dismissFocus()
        }
    }

    // Notifica o serviço e encerra a Activity
    private fun dismissFocus() {
        val intent = Intent(this, MainService::class.java).apply {
            action = ACTION_FOCUS_DISMISSED
        }
        startForegroundService(intent)
        finish()
    }
}