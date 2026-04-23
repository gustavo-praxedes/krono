package com.krono.app.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
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
import com.krono.app.ui.theme.KronoTokens
import kotlinx.coroutines.delay

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

    // ── Aplicação dos Tokens com Escala ─────────────────────
    val cornerRadius = (config.cornerRadius * scale).coerceAtMost(KronoTokens.Overlay.maxCornerRadiusFloat).dp
    val bgColor      = Color(config.backgroundColor).copy(alpha = config.bgOpacity)
    val txtColor     = Color(config.textColor).copy(alpha = config.textOpacity)
    val shape        = RoundedCornerShape(cornerRadius)

    val timeFontSize  = (KronoTokens.Overlay.timerFontSize.value * scale).sp
    val iconSizeDp    = (KronoTokens.Overlay.iconSize.value * scale).dp
    val btnSize       = (KronoTokens.Overlay.buttonSize.value * scale).dp
    val quickIconSize = (KronoTokens.Overlay.quickIconSize.value * scale).dp
    val quickBtnSize  = (KronoTokens.Overlay.quickBtnSize.value * scale).dp
    
    val paddingH      = (KronoTokens.Overlay.paddingH.value * scale).dp
    val paddingV      = (KronoTokens.Overlay.paddingV.value * scale).dp
    val btnTopPadding = (KronoTokens.Overlay.btnTopPadding.value * scale).dp
    val menuPaddingV  = (KronoTokens.Overlay.menuPaddingV.value * scale).dp
    val minColWidth   = (KronoTokens.Overlay.minWidth.value * scale).dp

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
            delay(KronoTokens.Overlay.menuTimeoutMs)
            menuVisible = false
        }
    }

    fun resetMenuTimer() { menuInteractionTick++ }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .clip(shape)
            .background(bgColor)
            .pointerInput(Unit) {
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
            .pointerInput(Unit) {
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
                                contentDescription = if (currentIsRunning) "Pausar" else "Iniciar",
                                tint               = if (currentIsRunning) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 1f),
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
                            Icon(Icons.Default.Refresh, "Reset", tint = txtColor, modifier = Modifier.size(iconSizeDp))
                        }

                        IconButton(
                            onClick  = {
                                menuVisible = false
                                currentOnSettings()
                            },
                            modifier = Modifier.size(btnSize)
                        ) {
                            Icon(Icons.Default.Settings, "Config", tint = txtColor, modifier = Modifier.size(iconSizeDp))
                        }

                        IconButton(
                            onClick  = onClose,
                            modifier = Modifier.size(btnSize)
                        ) {
                            Icon(Icons.Default.Close, "Fechar", tint = txtColor, modifier = Modifier.size(iconSizeDp))
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
                            IconButton(
                                onClick  = {
                                    resetMenuTimer()
                                    currentOnToggleFocus()
                                },
                                modifier = Modifier.size(quickBtnSize)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.TrackChanges,
                                    contentDescription = "Foco",
                                    tint               = if (config.focusModeEnabled) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
                                    modifier           = Modifier.size(quickIconSize)
                                )
                            }

                            IconButton(
                                onClick  = {
                                    resetMenuTimer()
                                    currentOnToggleKeepScreenOn()
                                },
                                modifier = Modifier.size(quickBtnSize)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.LightMode,
                                    contentDescription = "Tela Ligada",
                                    tint               = if (config.keepScreenOn) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
                                    modifier           = Modifier.size(quickIconSize)
                                )
                            }

                            IconButton(
                                onClick  = {
                                    resetMenuTimer()
                                    currentOnToggleAutoLaunch()
                                },
                                modifier = Modifier.size(quickBtnSize)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.OpenInNew,
                                    contentDescription = "Auto",
                                    tint               = if (config.autoLaunch) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
                                    modifier           = Modifier.size(quickIconSize)
                                )
                            }

                            IconButton(
                                onClick  = {
                                    resetMenuTimer()
                                    currentOnToggleBeep()
                                },
                                modifier = Modifier.size(quickBtnSize)
                            ) {
                                Icon(
                                    imageVector        = Icons.Default.VolumeUp,
                                    contentDescription = "Bip",
                                    tint               = if (config.isBeepEnabled) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
                                    modifier           = Modifier.size(quickIconSize)
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Menu",
                    tint = txtColor.copy(alpha = 0.4f),
                    modifier = Modifier
                        .height((10f * scale).dp)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount.y > 2f && !menuVisible) menuVisible = true
                                    onDrag(dragAmount.x, dragAmount.y)
                                    if (menuVisible) resetMenuTimer()
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    menuVisible = !menuVisible
                                    if (menuVisible) resetMenuTimer()
                                }
                            )
                        }
                )
            }
        }
    }
}
