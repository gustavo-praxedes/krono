package com.krono.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// ============================================================
// KronoTheme.kt — GIT 7
//
// 4 temas fixos + modo AUTO (segue o sistema).
// Seleção via selectedTheme: String no DataStore.
// ============================================================

enum class KronoThemeOption(val label: String) {
    AUTO           ("Automático"),
    DARK_MODERN    ("Dark Modern"),
    SOLARIZED_DARK ("Solarized Dark"),
    LIGHT_MODERN   ("Light Modern"),
    SOLARIZED_LIGHT("Solarized Light"),
}

// ── Tipografia Padrão ────────────────────────────────────────
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    )
)

// ============================================================
// KronoTokens — Design Token System
// Sistema centralizado de estilos para todos os diálogos.
// ============================================================

object KronoTokens {

    // ── Formas e Arredondamentos ─────────────────────────────
    object Shape {
        val dialog         = RoundedCornerShape(24.dp)
        val button         = RoundedCornerShape(16.dp)
        val buttonSmall    = RoundedCornerShape(12.dp)
        val card           = RoundedCornerShape(16.dp)
        val input          = RoundedCornerShape(12.dp)
        val badge          = RoundedCornerShape(8.dp)
        val progressBar    = RoundedCornerShape(50)
        val iconContainer  = RoundedCornerShape(12.dp)
    }

    // ── Tamanhos de Botões ───────────────────────────────────
    object Button {
        val height         = 56.dp
        val heightSmall    = 44.dp
        val iconSize       = 20.dp
        val iconSpacing    = 10.dp
        val paddingH       = 24.dp
    }

    // ── Espaçamentos ─────────────────────────────────────────
    object Spacing {
        val xs   = 4.dp
        val sm   = 8.dp
        val md   = 12.dp
        val lg   = 16.dp
        val xl   = 20.dp
        val xxl  = 24.dp
        val xxxl = 32.dp

        val dialogPadding   = 20.dp
        val dialogWidthFrac = 0.92f
        val listItemGap     = 10.dp
        val listIconGap     = 12.dp
        val sectionGap      = 20.dp
    }

    // ── Tipografia ───────────────────────────────────────────
    object Typography {
        val dialogTitle      = 22.sp
        val dialogSubtitle   = 14.sp
        val listItem         = 14.sp
        val buttonLabel      = 16.sp
        val buttonLabelSmall = 14.sp
        val statusLabel      = 12.sp
        val errorLabel       = 12.sp
        val bodyText         = 16.sp
    }

    // ── Tamanhos de Ícones ───────────────────────────────────
    object Icon {
        val listItem      = 20.dp
        val dialogHeader  = 24.dp
        val status        = 18.dp
        val small         = 16.dp
        val button        = 20.dp
        val close         = 32.dp
    }

    // ── Elevação e Sombras ───────────────────────────────────
    object Elevation {
        val dialog    = 6.dp
        val card      = 2.dp
        val flat      = 0.dp
    }

    // ── Espessuras de Linha ──────────────────────────────────
    object Stroke {
        val progressBar  = 8.dp
        val circularIndicator = 2.dp
        val divider      = 1.dp
        val cardBorder   = 1.dp
    }

    // ── Animações ────────────────────────────────────────────
    object Animation {
        val fadeDurationMs     = 200
        val toastDurationMs    = 3_000
        val menuAutoDismissMs  = 5_000
    }

    // ── Opacidades ───────────────────────────────────────────
    object Alpha {
        val divider   = 0.5f
        val disabled  = 0.38f
        val scrim     = 0.6f
    }

    // ── Tamanhos de Componentes Específicos ─────────────────
    object Component {
        val inlineSpinner  = 18.dp
        val buttonSpinner  = 20.dp
        val listItemHeight = 48.dp
    }

    // ── Overlay (Widget Flutuante) ──────────────────────────
    // Estética Antigravity: Espacialidade, Glassmorphism e Peso Visual.
    // ────────────────────────────────────────────────────────
    object Overlay {
        /** Arredondamento máximo permitido (evita deformação) */
        const val maxCornerRadiusFloat = 80f
        /** Arredondamento padrão inicial - Visual orgânico e premium */
        val defaultCornerRadius = 24.dp
        
        /** Largura mínima aumentada em ~20% para impacto visual */
        val minWidth       = 172.dp
        val paddingH       = 16.dp
        val paddingV       = 16.dp
        val btnSpacing     = 10.dp
        val btnTopPadding  = 8.dp
        val menuPaddingV   = 8.dp
        
        /** Tipografia Hero: impacto e legibilidade máxima */
        val timerFontSize  = 40.sp
        val iconSize       = 28.dp
        val buttonSize     = 28.dp
        
        /** Quick Menu: Alvos de toque melhorados (UX Pro Max) */
        val quickBtnSize   = 34.dp
        val quickIconSize  = 26.dp
        
        val menuTimeoutMs  = 5000L
    }
}

// ── Dark Modern ──────────────────────────────────────────────
private val DarkModernColors = darkColorScheme(
    primary          = Color(0xFF7B8CDE),
    onPrimary        = Color(0xFF1E1E2E),
    primaryContainer = Color(0xFF3A3A5C),
    secondary        = Color(0xFFE06C75),
    onSecondary      = Color(0xFF1E1E2E),
    background       = Color(0xFF1E1E2E),
    onBackground     = Color(0xFFCDD6F4),
    surface          = Color(0xFF2A2A3E),
    onSurface        = Color(0xFFCDD6F4),
    surfaceVariant   = Color(0xFF313244),
    onSurfaceVariant = Color(0xFF9399B2),
    outline          = Color(0xFF45475A),
)

// ... (Solarized Dark, Light Modern, Solarized Light permanecem iguais)
private val SolarizedDarkColors = darkColorScheme(
    primary          = Color(0xFF268BD2),
    onPrimary        = Color(0xFF002B36),
    primaryContainer = Color(0xFF073642),
    secondary        = Color(0xFF2AA198),
    onSecondary      = Color(0xFF002B36),
    background       = Color(0xFF002B36),
    onBackground     = Color(0xFF839496),
    surface          = Color(0xFF073642),
    onSurface        = Color(0xFF93A1A1),
    surfaceVariant   = Color(0xFF073642),
    onSurfaceVariant = Color(0xFF657B83),
    outline          = Color(0xFF586E75),
)

private val LightModernColors = lightColorScheme(
    primary          = Color(0xFF6B7FD4),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE1FF),
    secondary        = Color(0xFFD4546A),
    onSecondary      = Color(0xFFFFFFFF),
    background       = Color(0xFFFFFFFF),
    onBackground     = Color(0xFF1C1B1F),
    surface          = Color(0xFFF5F5F5),
    onSurface        = Color(0xFF1C1B1F),
    surfaceVariant   = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline          = Color(0xFFCAC4D0),
)

private val SolarizedLightColors = lightColorScheme(
    primary          = Color(0xFF2075C7),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD4E4F7),
    secondary        = Color(0xFF2AA198),
    onSecondary      = Color(0xFFFFFFFF),
    background       = Color(0xFFFDF6E3),
    onBackground     = Color(0xFF657B83),
    surface          = Color(0xFFEEE8D5),
    onSurface        = Color(0xFF586E75),
    surfaceVariant   = Color(0xFFE8E2CF),
    onSurfaceVariant = Color(0xFF839496),
    outline          = Color(0xFFD3C9A8),
)

@Composable
fun KronoTheme(
    selectedTheme: String,
    content: @Composable () -> Unit
) {
    val systemIsDark = isSystemInDarkTheme()

    val colorScheme = when (KronoThemeOption.entries.find { it.name == selectedTheme } ?: KronoThemeOption.AUTO) {
        KronoThemeOption.DARK_MODERN     -> DarkModernColors
        KronoThemeOption.SOLARIZED_DARK  -> SolarizedDarkColors
        KronoThemeOption.LIGHT_MODERN    -> LightModernColors
        KronoThemeOption.SOLARIZED_LIGHT -> SolarizedLightColors
        KronoThemeOption.AUTO            -> if (systemIsDark) DarkModernColors else LightModernColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}

/**
 * Retorna as cores do overlay (background, texto) como ARGB Int
 * para o tema selecionado, respeitando o modo claro/escuro do sistema
 * quando o tema for AUTO.
 */
fun overlayColorsForTheme(option: KronoThemeOption, systemIsDark: Boolean): Pair<Int, Int> {
    val effective = if (option == KronoThemeOption.AUTO) {
        if (systemIsDark) KronoThemeOption.DARK_MODERN else KronoThemeOption.LIGHT_MODERN
    } else option

    return when (effective) {
        KronoThemeOption.DARK_MODERN     -> Pair(0xFF1E1E2E.toInt(), 0xFFCDD6F4.toInt())
        KronoThemeOption.SOLARIZED_DARK  -> Pair(0xFF002B36.toInt(), 0xFF839496.toInt())
        KronoThemeOption.LIGHT_MODERN    -> Pair(0xFFFFFFFF.toInt(), 0xFF1C1B1F.toInt())
        KronoThemeOption.SOLARIZED_LIGHT -> Pair(0xFFFDF6E3.toInt(), 0xFF657B83.toInt())
        KronoThemeOption.AUTO            -> Pair(0xFF1E1E2E.toInt(), 0xFFCDD6F4.toInt()) // nunca alcançado
    }
}

