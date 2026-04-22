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
// Uso: KronoTokens.Dialog.shape, KronoTokens.Button.height, etc.
// ============================================================

object KronoTokens {

    // ── Formas e Arredondamentos ─────────────────────────────
    object Shape {
        /** Diálogos principais (ChangelogDialog, UpdateDialog, etc.) */
        val dialog         = RoundedCornerShape(24.dp)
        /** Botões primários e secundários */
        val button         = RoundedCornerShape(16.dp)
        /** Botões menores / chips */
        val buttonSmall    = RoundedCornerShape(12.dp)
        /** Cards internos dentro de diálogos */
        val card           = RoundedCornerShape(16.dp)
        /** Campos de input */
        val input          = RoundedCornerShape(12.dp)
        /** Tags / badges */
        val badge          = RoundedCornerShape(8.dp)
        /** Barra de progresso */
        val progressBar    = RoundedCornerShape(50)
        /** Ícones com fundo arredondado */
        val iconContainer  = RoundedCornerShape(12.dp)
    }

    // ── Tamanhos de Botões ───────────────────────────────────
    object Button {
        /** Altura padrão dos botões primários */
        val height         = 56.dp
        /** Altura dos botões secundários / menores */
        val heightSmall    = 44.dp
        /** Tamanho dos ícones dentro de botões */
        val iconSize       = 20.dp
        /** Espaço entre ícone e texto */
        val iconSpacing    = 10.dp
        /** Padding horizontal interno */
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

        /** Padding interno dos diálogos */
        val dialogPadding   = 20.dp
        /** Largura fracional do diálogo em relação à tela */
        val dialogWidthFrac = 0.92f
        /** Espaço entre itens de lista dentro do diálogo */
        val listItemGap     = 10.dp
        /** Espaço entre ícone e texto em itens de lista */
        val listIconGap     = 12.dp
        /** Gap entre seções dentro de um diálogo */
        val sectionGap      = 20.dp
    }

    // ── Tipografia ───────────────────────────────────────────
    object Typography {
        /** Título principal do diálogo */
        val dialogTitle      = 22.sp
        /** Subtítulo / versão */
        val dialogSubtitle   = 14.sp
        /** Corpo dos itens de lista */
        val listItem         = 14.sp
        /** Label de botão primário */
        val buttonLabel      = 16.sp
        /** Label de botão pequeno */
        val buttonLabelSmall = 14.sp
        /** Texto de status (ex: "Baixando: 45%") */
        val statusLabel      = 12.sp
        /** Texto de erro */
        val errorLabel       = 12.sp
        /** Corpo de texto em diálogos */
        val bodyText = 16.sp
    }

    // ── Tamanhos de Ícones ───────────────────────────────────
    object Icon {
        /** Ícones em itens de lista */
        val listItem      = 20.dp
        /** Ícone no título do diálogo */
        val dialogHeader  = 24.dp
        /** Ícone de status (ex: CheckCircle) */
        val status        = 18.dp
        /** Ícone pequeno (ex: em badges ou labels) */
        val small         = 16.dp
        /** Ícone em botões primários */
        val button        = 20.dp
        /** Ícone de fechar (X) */
        val close         = 32.dp
    }

    // ── Elevação e Sombras ───────────────────────────────────
    object Elevation {
        /** Elevação de superfície dos diálogos */
        val dialog    = 6.dp
        /** Elevação de cards internos */
        val card      = 2.dp
        /** Elevação zero (flat) */
        val flat      = 0.dp
    }

    // ── Espessuras de Linha ──────────────────────────────────
    object Stroke {
        /** Barra de progresso linear */
        val progressBar  = 8.dp
        /** Indicador circular de loading */
        val circularIndicator = 2.dp
        /** Divisores / separadores */
        val divider      = 1.dp
        /** Bordas de cards */
        val cardBorder   = 1.dp
    }

    // ── Animações ────────────────────────────────────────────
    object Animation {
        /** Duração padrão de transições de fade */
        val fadeDurationMs     = 200
        /** Duração de mensagens temporárias (ex: "Download iniciado") */
        val toastDurationMs    = 3_000
        /** Duração do fechamento do menu rápido por inatividade */
        val menuAutoDismissMs  = 5_000
    }

    // ── Opacidades ───────────────────────────────────────────
    object Alpha {
        /** Divisores e bordas sutis */
        val divider   = 0.5f
        /** Itens desabilitados */
        val disabled  = 0.38f
        /** Overlay de fundo de modais */
        val scrim     = 0.6f
    }

    // ── Tamanhos de Componentes Específicos ─────────────────
    object Component {
        /** Tamanho do CircularProgressIndicator inline */
        val inlineSpinner  = 18.dp
        /** Tamanho do CircularProgressIndicator de botão */
        val buttonSpinner  = 20.dp
        /** Altura mínima de item de lista clicável */
        val listItemHeight = 48.dp
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
        typography  = AppTypography,
        content     = content
    )
}