package com.krono.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krono.app.data.TimerState
import com.krono.app.data.toFormattedTime

// ============================================================
// TimerScreen.kt
// Tela principal do app — espelho visual do TimerViewModel.
// Não tem lógica própria de tempo — apenas observa e exibe
// o mesmo estado que o overlay e a notificação usam.
// ============================================================

@Composable
fun TimerScreen(
    timerState       : TimerState,
    onStart          : () -> Unit,
    onPause          : () -> Unit,
    onReset          : () -> Unit,
    onOpenOverlay    : () -> Unit,
    onOpenSettings   : () -> Unit
) {
    val isRunning = timerState.isRunning

    Scaffold(
        topBar = {
            // Barra superior com engrenagem de configurações
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        text       = "Krono",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector        = Icons.Default.Settings,
                            contentDescription = "Configurações"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(Modifier.weight(1f))

            // ── Display do Tempo ──────────────────────────────
            Text(
                text       = timerState.elapsedMs.toFormattedTime(
                    showHours   = true,
                    showSeconds = true
                ),
                fontSize   = 64.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color      = MaterialTheme.colorScheme.onBackground
            )

            // ── Indicador de limite ───────────────────────────
            if (timerState.isAtLimit) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = "LIMITE ATINGIDO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(48.dp))

            // ── Botões Play / Pause / Reset ───────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Reset
                FilledTonalIconButton(
                    onClick  = onReset,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier           = Modifier.size(28.dp)
                    )
                }

                // Play / Pause — botão maior e destaque
                FilledIconButton(
                    onClick  = { if (isRunning) onPause() else onStart() },
                    enabled  = !timerState.isAtLimit,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pausar" else "Iniciar",
                        modifier           = Modifier.size(40.dp)
                    )
                }

                // Placeholder para simetria visual
                Spacer(Modifier.size(56.dp))
            }

            Spacer(Modifier.weight(1f))

            // ── Botão Iniciar Overlay ─────────────────────────
            Button(
                onClick  = onOpenOverlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text       = "Iniciar Overlay",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}