package com.krono.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val QUICK_MENU_TIMEOUT_MS = 4_000L

@Composable
fun FloatingTimerUi(
    timerState            : TimerState,
    config                : OverlayConfig,
    onStart               : () -> Unit,
    onPause               : () -> Unit,
    onReset               : () -> Unit,
    onDrag                : (dx: Float, dy: Float) -> Unit,
    onDragEnd             : () -> Unit,
    onClose               : () -> Unit,
    onSettings            : () -> Unit,
    onToggleFocus         : () -> Unit,
    onToggleKeepScreenOn  : () -> Unit,
    onToggleAutoLaunch    : () -> Unit,
    onToggleBeep          : () -> Unit,
    onMenuVisibilityChange: (Boolean) -> Unit
) {
    val isRunning = timerState.isRunning
    val scale     = config.scale

    val cornerRadius = (config.cornerRadius * scale).coerceAtMost(64f).dp
    val bgColor      = Color(config.backgroundColor).copy(alpha = config.bgOpacity)
    val txtColor     = Color(config.textColor).copy(alpha = config.textOpacity)
    val shape        = RoundedCornerShape(cornerRadius)

    val timeFontSize  = (32f * scale).sp
    val iconSizeDp    = (24f * scale).dp
    val btnSize       = (24f * scale).dp
    val quickIconSize = (24f * scale).dp
    val quickBtnSize  = (24f * scale).dp
    val paddingH      = (12f * scale).dp
    val paddingV      = (12f * scale).dp
    val btnSpacing    = (4f  * scale).dp
    val btnTopPadding = (4f  * scale).dp
    val limitFontSize = (10f * scale).sp
    val menuPaddingV  = (4f  * scale).dp
    val menuSpacing   = (12f * scale).dp
    val minColWidth   = (144f * scale).dp

    val currentOnStart              by rememberUpdatedState(onStart)
    val currentOnPause              by rememberUpdatedState(onPause)
    val currentOnReset              by rememberUpdatedState(onReset)
    val currentOnSettings           by rememberUpdatedState(onSettings)
    val currentOnToggleFocus        by rememberUpdatedState(onToggleFocus)
    val currentOnToggleKeepScreenOn by rememberUpdatedState(onToggleKeepScreenOn)
    val currentOnToggleAutoLaunch   by rememberUpdatedState(onToggleAutoLaunch)
    val currentOnToggleBeep         by rememberUpdatedState(onToggleBeep)
    val currentIsRunning            by rememberUpdatedState(isRunning)

    var menuVisible by remember { mutableStateOf(false) }

    LaunchedEffect(menuVisible) {
        onMenuVisibilityChange(menuVisible)
    }

    var menuInteractionTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(menuVisible, menuInteractionTick) {
        if (menuVisible) {
            delay(QUICK_MENU_TIMEOUT_MS)
            menuVisible = false
        }
    }

    fun resetMenuTimer() { menuInteractionTick++ }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding((8f * scale).dp) // Reserva espaço da Window pro shadow respirar
            .shadow(
                elevation = (6f * scale).dp, 
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.3f), // Opcional, dá mais volume na sombra
                spotColor = Color.Black.copy(alpha = 0.8f)
            )
            .clip(shape)
            .background(bgColor)
            .pointerInput(Unit) {
                coroutineScope {
                    launch {
                        detectDragGestures(
                            onDragEnd    = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag       = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                                if (menuVisible) resetMenuTimer()
                            }
                        )
                    }
                    launch {
                        detectTapGestures(
                            onTap = {
                                if (menuVisible) {
                                    menuVisible = false
                                } else {
                                    if (currentIsRunning) currentOnPause() else currentOnStart()
                                }
                            },
                            onDoubleTap = {
                                menuVisible = false
                                currentOnReset()
                            }
                        )
                    }
                }
            }
    ) {
        Column(
            modifier            = Modifier
                .widthIn(min = minColWidth)
                .padding(horizontal = paddingH, vertical = paddingV),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                text       = timerState.elapsedMs.toFormattedTime(
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

            val MainButtonRow = @Composable {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(top = btnTopPadding),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick  = {
                            if (menuVisible) resetMenuTimer()
                            if (currentIsRunning) currentOnPause() else currentOnStart()
                        },
                        enabled  = !timerState.isAtLimit,
                        modifier = Modifier.size(btnSize)
                    ) {
                        Icon(
                            imageVector        = if (currentIsRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint               = if (currentIsRunning) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
                            modifier           = Modifier.size(iconSizeDp)
                        )
                    }

                    IconButton(
                        onClick  = {
                            if (menuVisible) resetMenuTimer()
                            currentOnReset()
                        },
                        modifier = Modifier.size(btnSize)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = txtColor, modifier = Modifier.size(iconSizeDp))
                    }

                    IconButton(
                        onClick  = {
                            menuVisible = false
                            currentOnSettings()
                        },
                        modifier = Modifier.size(btnSize)
                    ) {
                        Icon(Icons.Default.Settings, null, tint = txtColor, modifier = Modifier.size(iconSizeDp))
                    }

                    IconButton(
                        onClick  = onClose,
                        modifier = Modifier.size(btnSize)
                    ) {
                        Icon(Icons.Default.Close, null, tint = txtColor, modifier = Modifier.size(iconSizeDp))
                    }
                }
            }

            if (config.showButtons) {
                MainButtonRow()
            } else {
                AnimatedVisibility(
                    visible = menuVisible,
                    modifier = Modifier.fillMaxWidth(),
                    enter = expandVertically(expandFrom = Alignment.Top) + androidx.compose.animation.fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + androidx.compose.animation.fadeOut()
                ) {
                    MainButtonRow()
                }
            }

            // ── Menu de Configurações Rápidas ────────────────────
            AnimatedVisibility(
                visible = menuVisible,
                modifier = Modifier.fillMaxWidth(),
                enter = expandVertically(expandFrom = Alignment.Top) + androidx.compose.animation.fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + androidx.compose.animation.fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = menuPaddingV),
                        thickness = 0.5.dp,
                        color = txtColor.copy(alpha = 0.2f)
                    )
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(vertical = menuPaddingV),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        // 🎯 Toggle Modo Foco
                        IconButton(
                            onClick  = {
                                resetMenuTimer()
                                currentOnToggleFocus()
                            },
                            modifier = Modifier.size(quickBtnSize)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.TrackChanges,
                                contentDescription = "Modo Foco",
                                tint               = if (config.focusModeEnabled) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
                                modifier           = Modifier.size(quickIconSize)
                            )
                        }

                        // 💡 Toggle Manter Tela Ligada
                        IconButton(
                            onClick  = {
                                resetMenuTimer()
                                currentOnToggleKeepScreenOn()
                            },
                            modifier = Modifier.size(quickBtnSize)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.LightMode,
                                contentDescription = "Manter Tela Ligada",
                                tint               = if (config.keepScreenOn) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
                                modifier           = Modifier.size(quickIconSize)
                            )
                        }

                        // 🚀 Toggle Abrir Diretamente
                        IconButton(
                            onClick  = {
                                resetMenuTimer()
                                currentOnToggleAutoLaunch()
                            },
                            modifier = Modifier.size(quickBtnSize)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.OpenInNew,
                                contentDescription = "Abrir Diretamente",
                                tint               = if (config.autoLaunch) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
                                modifier           = Modifier.size(quickIconSize)
                            )
                        }

                        // 🔔 Toggle Bip
                        IconButton(
                            onClick  = {
                                resetMenuTimer()
                                currentOnToggleBeep()
                            },
                            modifier = Modifier.size(quickBtnSize)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.VolumeUp,
                                contentDescription = "Bipe Ativo",
                                tint               = if (config.isBeepEnabled) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
                                modifier           = Modifier.size(quickIconSize)
                            )
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Expandir Menu",
                tint = txtColor.copy(alpha = 0.4f),
                modifier = Modifier
                    .height((20f * scale).dp)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        coroutineScope {
                            launch {
                                detectDragGestures(
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragEnd() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        if (dragAmount.y > 1f && !menuVisible) {
                                            menuVisible = true
                                        }
                                        onDrag(dragAmount.x, dragAmount.y)
                                        if (menuVisible) resetMenuTimer()
                                    }
                                )
                            }
                            launch {
                                detectTapGestures(
                                    onTap = {
                                        menuVisible = !menuVisible
                                        if (menuVisible) resetMenuTimer()
                                    }
                                )
                            }
                        }
                    }
            )
            }
        }
    }
}