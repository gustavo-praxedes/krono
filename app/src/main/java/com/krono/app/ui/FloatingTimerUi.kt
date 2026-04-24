package com.krono.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krono.app.data.OverlayConfig
import com.krono.app.data.TimerState
import com.krono.app.data.toFormattedTime
import com.krono.app.ui.theme.KronoTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btnScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 1f,
        animationSpec = tween(150),
        label = "btnAlpha"
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        },
        enabled = enabled,
        interactionSource = interactionSource,
        content = content
    )
}

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

    // ── Animação de Entrada Premium (Escala + Alpha estável) ──────────
    val entranceScale = remember { Animatable(0.88f) }
    val entranceAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Disparamos ambas em paralelo para um efeito fluido
        launch {
            entranceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.40f, // Mantém o overshoot elegante
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            entranceAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
            )
        }
    }

    // ── Escalonamento Dinâmico de Tokens ─────────────────────
    val currentScale  = entranceScale.value
    val cornerRadius  = (config.cornerRadius * scale * currentScale).coerceAtMost(KronoTokens.Overlay.maxCornerRadiusFloat).dp
    val bgColor       = Color(config.backgroundColor).copy(alpha = config.bgOpacity)
    val txtColor      = Color(config.textColor).copy(alpha = config.textOpacity)
    val shape         = RoundedCornerShape(cornerRadius)

    val timeFontSize  = (KronoTokens.Overlay.timerFontSize.value * scale * currentScale).sp
    val iconSizeDp    = (KronoTokens.Overlay.iconSize.value * scale * currentScale).dp
    val btnSize       = (KronoTokens.Overlay.buttonSize.value * scale * currentScale).dp
    val quickIconSize = (KronoTokens.Overlay.quickIconSize.value * scale * currentScale).dp
    val quickBtnSize  = (KronoTokens.Overlay.quickBtnSize.value * scale * currentScale).dp

    val paddingH      = (KronoTokens.Overlay.paddingH.value * scale * currentScale).dp
    val paddingV      = (KronoTokens.Overlay.paddingV.value * scale * currentScale).dp
    val btnTopPadding = (KronoTokens.Overlay.btnTopPadding.value * scale * currentScale).dp
    val menuPaddingV  = (KronoTokens.Overlay.menuPaddingV.value * scale * currentScale).dp
    val minColWidth   = (KronoTokens.Overlay.minWidth.value * scale * currentScale).dp

    val currentOnStart              by rememberUpdatedState(onStart)
    val currentOnPause              by rememberUpdatedState(onPause)
    val currentOnReset              by rememberUpdatedState(onReset)
    val currentOnSettings           by rememberUpdatedState(onSettings)
    val currentIsRunning            by rememberUpdatedState(isRunning)

    var menuVisible by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
        label = "dragScale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else txtColor.copy(alpha = 0.15f),
        animationSpec = tween(600),
        label = "borderColor"
    )

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
            .graphicsLayer {
                val finalScale = currentScale * dragScale
                scaleX = finalScale
                scaleY = finalScale
                alpha = entranceAlpha.value
                this.shape = shape
                clip = true // Ativamos o clip aqui para garantir cantos perfeitos na animação
            }
            // Aplicamos o background e border usando o mesmo shape para evitar aliasing (bordas pretas)
            .background(bgColor, shape)
            .border(
                width = 0.86.dp,
                color = borderColor,
                shape = shape
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart  = { isDragging = true },
                    onDragEnd    = { isDragging = false; onDragEnd() },
                    onDragCancel = { isDragging = false; onDragEnd() },
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
                        AnimatedIconButton(
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

                        AnimatedIconButton(
                            onClick  = {
                                if (menuVisible) resetMenuTimer()
                                currentOnReset()
                            },
                            modifier = Modifier.size(btnSize)
                        ) {
                            Icon(Icons.Default.Refresh, "Reset", tint = txtColor, modifier = Modifier.size(iconSizeDp))
                        }

                        AnimatedIconButton(
                            onClick  = {
                                menuVisible = false
                                currentOnSettings()
                            },
                            modifier = Modifier.size(btnSize)
                        ) {
                            Icon(Icons.Default.Settings, "Config", tint = txtColor, modifier = Modifier.size(iconSizeDp))
                        }

                        AnimatedIconButton(
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
                        enter = expandVertically(expandFrom = Alignment.Top, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) + fadeOut()
                    ) {
                        MainButtonRow()
                    }
                }

                // ── Menu de Configurações Rápidas ────────────────────
                AnimatedVisibility(
                    visible = menuVisible,
                    modifier = Modifier.fillMaxWidth(),
                    enter = expandVertically(expandFrom = Alignment.Top, animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow)) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)) + fadeOut()
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
                            QuickOptionIcon(Icons.Default.TrackChanges, config.focusModeEnabled, txtColor, quickBtnSize, quickIconSize) {
                                resetMenuTimer(); onToggleFocus()
                            }
                            QuickOptionIcon(Icons.Default.LightMode, config.keepScreenOn, txtColor, quickBtnSize, quickIconSize) {
                                resetMenuTimer(); onToggleKeepScreenOn()
                            }
                            QuickOptionIcon(Icons.Default.OpenInNew, config.autoLaunch, txtColor, quickBtnSize, quickIconSize) {
                                resetMenuTimer(); onToggleAutoLaunch()
                            }
                            QuickOptionIcon(Icons.Default.VolumeUp, config.isBeepEnabled, txtColor, quickBtnSize, quickIconSize) {
                                resetMenuTimer(); onToggleBeep()
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Menu",
                    tint = txtColor.copy(alpha = 0.4f),
                    modifier = Modifier
                        .height((10f * scale * currentScale).dp)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart  = { isDragging = true },
                                onDragEnd = { isDragging = false; onDragEnd() },
                                onDragCancel = { isDragging = false; onDragEnd() },
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

@Composable
private fun QuickOptionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    txtColor: Color,
    btnSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    AnimatedIconButton(
        onClick  = onClick,
        modifier = Modifier.size(btnSize)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (isActive) MaterialTheme.colorScheme.primary else txtColor.copy(alpha = 0.4f),
            modifier           = Modifier.size(iconSize)
        )
    }
}