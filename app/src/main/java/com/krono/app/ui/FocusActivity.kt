package com.krono.app.ui

import android.app.Activity
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

// ============================================================
// FocusActivity.kt
// Activity fullscreen com fundo preto para o Modo Foco.
//
// Comportamento:
//   • Tela fica completamente preta
//   • O overlay do MainService continua visível por cima
//   • Toque na área preta encerra o Modo Foco
//   • Toque no cronômetro é tratado pelo overlay normalmente
// ============================================================

class FocusActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen sem barra de status e navegação
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            // Fundo preto imediato — evita flash branco ao abrir
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor  = android.graphics.Color.BLACK
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
                                // Toque na área preta encerra o Modo Foco
                                onTap = { finish() }
                            )
                        }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Encerra o Modo Foco se o usuário sair por outro meio
        // (botão home, notificação, etc.)
        finish()
    }
}