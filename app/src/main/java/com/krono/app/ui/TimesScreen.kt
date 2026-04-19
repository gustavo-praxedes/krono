package com.krono.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Krono",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            // ── Display do Tempo ──────────────────────────────
            Text(
                text = timerState.elapsedMs.toFormattedTime(
                    showHours = true,
                    showSeconds = true
                ),
                fontSize = 76.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.animateContentSize()
            )

            if (timerState.isAtLimit) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "LIMITE ATINGIDO",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Botões de controle ────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = onReset,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(28.dp)
                    )
                }

                FilledIconButton(
                    onClick = { if (isRunning) onPause() else onStart() },
                    enabled = !timerState.isAtLimit,
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Crossfade(
                        targetState = isRunning, 
                        animationSpec = tween(durationMillis = 300),
                        label = "play_pause_anim"
                    ) { running ->
                        Icon(
                            imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (running) "Pausar" else "Iniciar",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                FilledTonalIconButton(
                    onClick = onOpenOverlay,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Abrir Overlay",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(64.dp))
        }
    }
}