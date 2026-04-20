package com.krono.app.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krono.app.ACTION_START_FOCUS
import com.krono.app.data.OverlayConfig
import com.krono.app.data.OverlayDataStore
import com.krono.app.service.MainService
import com.krono.app.util.UpdateInfo
import kotlinx.coroutines.launch

// ============================================================
// SettingsScreen.kt
// Tela de configurações do Krono.
//
// Recebe da MainActivity apenas o que não pode ser resolvido
// localmente: acesso ao DataStore, callbacks de serviço e
// a flag de doação pendente.
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dataStore         : OverlayDataStore,
    pendingUpdateInfo : UpdateInfo?,
    showDonationDialog: Boolean = false,
    isServiceRunning  : () -> Boolean,
    onStartFocusMode  : () -> Unit,
    onShowOverlay     : () -> Unit,
    onBack            : () -> Unit
) {
    val config = dataStore.configFlow.collectAsState(initial = OverlayConfig()).value
    val scope  = rememberCoroutineScope()

    // ── Estados dos diálogos ──────────────────────────────────
    var showBgPicker          by remember { mutableStateOf(false) }
    var showTextPicker        by remember { mutableStateOf(false) }
    var showAboutDialog       by remember { mutableStateOf(false) }
    var showDonation          by remember { mutableStateOf(showDonationDialog) }
    var showDonationFromAbout by remember { mutableStateOf(false) }

    // Fluxo: AboutDialog → ChangelogDialog → UpdateDialog
    var changelogInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateInfo    by remember { mutableStateOf<UpdateInfo?>(pendingUpdateInfo) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text       = "Configurações",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
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
                .padding(horizontal = 24.dp)
        ) {
            // ── Conteúdo rolável ──────────────────────────────
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // ── Toggles ───────────────────────────────────
                ToggleRow("Abrir Diretamente", config.autoLaunch) {
                    scope.launch { dataStore.updateConfig(config.copy(autoLaunch = it)) }
                }
                ToggleRow("Exibir Horas", config.showHours) {
                    if (!it && !config.showSeconds) return@ToggleRow
                    scope.launch { dataStore.updateConfig(config.copy(showHours = it)) }
                }
                ToggleRow("Exibir Segundos", config.showSeconds) {
                    if (!it && !config.showHours) return@ToggleRow
                    scope.launch { dataStore.updateConfig(config.copy(showSeconds = it)) }
                }
                ToggleRow("Exibir Botões", config.showButtons) {
                    scope.launch { dataStore.updateConfig(config.copy(showButtons = it)) }
                }
                ToggleRow("Manter Tela Ligada", config.keepScreenOn) {
                    scope.launch { dataStore.updateConfig(config.copy(keepScreenOn = it)) }
                }
                ToggleRow("Modo Foco", config.focusModeEnabled) { isEnabled ->
                    scope.launch { dataStore.updateConfig(config.copy(focusModeEnabled = isEnabled)) }
                    if (isEnabled && isServiceRunning()) onStartFocusMode()
                }
                ToggleRow("Bipe Ativo", config.isBeepEnabled) {
                    scope.launch { dataStore.updateConfig(config.copy(isBeepEnabled = it)) }
                }
                ToggleRow("Vibração Ativa", config.isVibrationEnabled) {
                    scope.launch { dataStore.updateConfig(config.copy(isVibrationEnabled = it)) }
                }

                Spacer(Modifier.height(16.dp))

                // ── Tempo Limite ──────────────────────────────
                TimeLimitField(
                    timeLimitSeconds = config.timeLimitSeconds,
                    onConfirm        = { seconds ->
                        scope.launch { dataStore.updateConfig(config.copy(timeLimitSeconds = seconds)) }
                    }
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // ── Seção APARÊNCIA ───────────────────────────
                Text(
                    text       = "APARÊNCIA",
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color      = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.padding(bottom = 12.dp)
                )

                ThemeSelector(
                    selectedTheme = config.selectedTheme,
                    onChange      = { theme ->
                        scope.launch { dataStore.updateConfig(config.copy(selectedTheme = theme)) }
                    }
                )

                Spacer(Modifier.height(12.dp))

                ColorRow(
                    label   = "Cor de Fundo",
                    color   = Color(config.backgroundColor).copy(alpha = config.bgOpacity),
                    onClick = { showBgPicker = true }
                )
                Spacer(Modifier.height(8.dp))
                ColorRow(
                    label   = "Cor do Texto",
                    color   = Color(config.textColor).copy(alpha = config.textOpacity),
                    onClick = { showTextPicker = true }
                )

                Spacer(Modifier.height(24.dp))

                AppearanceSlider(
                    label    = "Escala do Widget",
                    value    = config.scale,
                    minLabel = "0.5x",
                    maxLabel = "1.5x",
                    range    = 0.5f..1.5f,
                    display  = "${String.format("%.1f", config.scale)}x",
                    onChange = { scope.launch { dataStore.updateConfig(config.copy(scale = it)) } }
                )

                Spacer(Modifier.height(12.dp))

                AppearanceSlider(
                    label    = "Arredondamento",
                    value    = config.cornerRadius,
                    minLabel = "0dp",
                    maxLabel = "50dp",
                    range    = 0f..50f,
                    display  = "${config.cornerRadius.toInt()}dp",
                    onChange = { scope.launch { dataStore.updateConfig(config.copy(cornerRadius = it)) } }
                )

                Spacer(Modifier.height(48.dp))
            }

            // ── Rodapé ────────────────────────────────────────
            TextButton(
                onClick  = { showAboutDialog = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text(
                    text       = "Sobre o App",
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // ── Diálogos ──────────────────────────────────────────────

    if (showBgPicker) {
        ColorPickerDialog(
            title          = "Cor de Fundo",
            initialColor   = Color(config.backgroundColor),
            initialOpacity = config.bgOpacity,
            onPreview      = { color, opacity ->
                scope.launch {
                    dataStore.updateConfig(config.copy(backgroundColor = color.toArgb(), bgOpacity = opacity))
                }
            },
            onConfirm = { color, opacity ->
                scope.launch {
                    dataStore.updateConfig(config.copy(backgroundColor = color.toArgb(), bgOpacity = opacity))
                }
                showBgPicker = false
            },
            onDismiss = { showBgPicker = false }
        )
    }

    if (showTextPicker) {
        ColorPickerDialog(
            title          = "Cor do Texto",
            initialColor   = Color(config.textColor),
            initialOpacity = config.textOpacity,
            onPreview      = { color, opacity ->
                scope.launch {
                    dataStore.updateConfig(config.copy(textColor = color.toArgb(), textOpacity = opacity))
                }
            },
            onConfirm = { color, opacity ->
                scope.launch {
                    dataStore.updateConfig(config.copy(textColor = color.toArgb(), textOpacity = opacity))
                }
                showTextPicker = false
            },
            onDismiss = { showTextPicker = false }
        )
    }

    if (showDonation) {
        DonationDialog(
            onDismiss = {
                showDonation = false
                if (isServiceRunning()) onShowOverlay()
            },
            onDonate = {
                showDonation = false
                onShowOverlay()
            }
        )
    }

    if (showDonationFromAbout) {
        DonationDialog(
            onDismiss = { showDonationFromAbout = false },
            onDonate  = { showDonationFromAbout = false }
        )
    }

    // ── Fluxo: Sobre → Changelog → Update ─────────────────────

    if (showAboutDialog) {
        AboutDialog(
            onDismiss       = { showAboutDialog = false },
            onSupportClick  = {
                showAboutDialog       = false
                showDonationFromAbout = true
            },
            onShowChangelog = { info ->
                showAboutDialog = false
                changelogInfo   = info
            }
        )
    }

    if (changelogInfo != null) {
        ChangelogDialog(
            updateInfo        = changelogInfo!!,
            onDismiss         = { changelogInfo = null },
            onUpdateAvailable = { info ->
                changelogInfo = null
                updateInfo    = info
            }
        )
    }

    if (updateInfo != null) {
        UpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss  = { updateInfo = null }
        )
    }
}
