package com.krono.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krono.app.data.OverlayConfig
import com.krono.app.data.TimerState
import com.krono.app.data.toFormattedTime
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun FloatingTimerUi(
    timerState : TimerState,
    config     : OverlayConfig,
    onStart    : () -> Unit,
    onPause    : () -> Unit,
    onReset    : () -> Unit,
    onDrag     : (dx: Float, dy: Float) -> Unit,
    onDragEnd  : () -> Unit,
    onClose    : () -> Unit,
    onSettings : () -> Unit
) {
    val isRunning    = timerState.isRunning
    val scale        = config.scale

    // Agora o corner radius e todos os tamanhos reagem à escala proporcionalmente
    val cornerRadius = (config.cornerRadius * scale).coerceAtMost(64f).dp

    val bgColor  = Color(config.backgroundColor).copy(alpha = config.bgOpacity)
    val txtColor = Color(config.textColor).copy(alpha = config.textOpacity)
    val shape = RoundedCornerShape(cornerRadius)

    // ── Tamanhos PROPORCIONAIS (Multiplicamos pela escala aqui) ──
    // Isso garante que o "box" real do Android diminua junto com o desenho
    val timeFontSize  = (32f * scale).sp
    val iconSizeDp    = (24f * scale).dp
    val btnSize       = (40f * scale).dp
    val paddingH      = (16f * scale).dp
    val paddingV      = (12f * scale).dp
    val btnSpacing    = (4f  * scale).dp
    val btnTopPadding = (8f  * scale).dp
    val limitFontSize = (10f * scale).sp

    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnReset by rememberUpdatedState(onReset)
    val currentOnSettings by rememberUpdatedState(onSettings)
    val currentIsRunning by rememberUpdatedState(isRunning)

    // Removido o Box externo com dynamicPadding que causava o distanciamento
    Box(
        modifier = Modifier
            .wrapContentSize()
            .clip(shape)
            .background(bgColor)
            .pointerInput(Unit) {
                coroutineScope {
                    launch {
                        detectDragGestures(
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        )
                    }
                    launch {
                        detectTapGestures(
                            onTap = {
                                if (currentIsRunning) currentOnPause() else currentOnStart() },
                            onDoubleTap = { currentOnReset() },
                            onLongPress = { currentOnSettings() }
                        )
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(horizontal = paddingH, vertical = paddingV),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timerState.elapsedMs.toFormattedTime(
                    showHours   = config.showHours,
                    showSeconds = config.showSeconds
                ),
                color      = txtColor,
                fontSize   = timeFontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines   = 1,
                softWrap   = false
            )

            if (timerState.isAtLimit) {
                Text(
                    text       = "LIMITE",
                    color      = txtColor.copy(alpha = 0.8f),
                    fontSize   = limitFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (config.showButtons) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(top = btnTopPadding),
                    horizontalArrangement = Arrangement.spacedBy(btnSpacing),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick  = { if (isRunning) currentOnPause() else currentOnStart() },
                        enabled  = !timerState.isAtLimit,
                        modifier = Modifier.size(btnSize)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (timerState.isAtLimit) txtColor.copy(alpha = 0.4f) else txtColor,
                            modifier = Modifier.size(iconSizeDp)
                        )
                    }

                    IconButton(onClick = currentOnReset, modifier = Modifier.size(btnSize)) {
                        Icon(Icons.Default.Refresh, null, tint = txtColor, modifier = Modifier.size(iconSizeDp))
                    }

                    IconButton(onClick = currentOnSettings, modifier = Modifier.size(btnSize)) {
                        Icon(Icons.Default.Settings, null, tint = txtColor, modifier = Modifier.size(iconSizeDp))
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(btnSize)) {
                        Icon(Icons.Default.Close, null, tint = txtColor, modifier = Modifier.size(iconSizeDp))
                    }
                }
            }
        }
    }
}