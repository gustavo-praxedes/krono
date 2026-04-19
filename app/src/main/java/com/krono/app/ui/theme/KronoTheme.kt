package com.krono.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

// ── Solarized Dark ───────────────────────────────────────────
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

// ── Light Modern ─────────────────────────────────────────────
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

// ── Solarized Light ──────────────────────────────────────────
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
        content     = content
    )
}