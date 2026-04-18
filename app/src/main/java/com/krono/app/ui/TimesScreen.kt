package com.krono.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
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

@Composable
fun TimerScreen(
    timerState     : TimerState,
    onStart        : () -> Unit,
    onPause        : () -> Unit,
    onReset        : () -> Unit,
    onOpenOverlay  : () -> Unit,
    onOpenSettings : () -> Unit
) {
    val isRunning = timerState.isRunning

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        text = "Krono",
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                    )
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1/4 superior — espaço acima do cronômetro
            Spacer(Modifier.fillMaxHeight(0.10f))

            // ── Display do Tempo ──────────────────────────────
            Text(
                text = timerState.elapsedMs.toFormattedTime(
                    showHours = true,
                    showSeconds = true
                ),
                fontSize = 70.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                softWrap = false
            )

            if (timerState.isAtLimit) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "LIMITE ATINGIDO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Empurra os botões para o centro/baixo
            Spacer(Modifier.height(20.dp))

            // ── Botões de controle ────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onReset,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(32.dp)
                    )
                }

                FilledIconButton(
                    onClick = { if (isRunning) onPause() else onStart() },
                    enabled = !timerState.isAtLimit,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pausar" else "Iniciar",
                        modifier = Modifier.size(32.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onOpenOverlay,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Abrir Overlay",
                        modifier = Modifier.size(32.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(60.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        modifier = Modifier.size(25.dp)
                    )
                }
            }

            /*actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        modifier = Modifier.size(25.dp)
                    )
                }
            }*/



            Spacer(Modifier.height(48.dp))
        }
    }
}