package com.krono.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krono.app.R
import com.krono.app.data.OverlayConfig
import com.krono.app.data.OverlayDataStore
import com.krono.app.ui.theme.KronoThemeOption
import com.krono.app.ui.theme.overlayColorsForTheme
import com.krono.app.util.UpdateInfo
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dataStore         : OverlayDataStore,
    pendingUpdateInfo : UpdateInfo?,
    isServiceRunning  : () -> Boolean,
    onStartFocusMode  : () -> Unit,
    onShowOverlay     : () -> Unit,
    onBack            : () -> Unit
) {
    val config      = dataStore.configFlow.collectAsState(initial = OverlayConfig()).value
    val scope        = rememberCoroutineScope()
    val systemIsDark = isSystemInDarkTheme()

    var showBgPicker          by remember { mutableStateOf(false) }
    var showTextPicker        by remember { mutableStateOf(false) }
    var showAboutDialog       by remember { mutableStateOf(false) }
    val context               = androidx.compose.ui.platform.LocalContext.current

    var changelogInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateInfo    by remember { mutableStateOf<UpdateInfo?>(pendingUpdateInfo) }

    LaunchedEffect(pendingUpdateInfo) {
        if (pendingUpdateInfo != null) {
            updateInfo = pendingUpdateInfo
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text       = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
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
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                ToggleRow(stringResource(R.string.label_auto_launch), config.autoLaunch) {
                    scope.launch { dataStore.updateConfig(config.copy(autoLaunch = it)) }
                }
                ToggleRow(stringResource(R.string.label_show_hours), config.showHours) {
                    if (!it && !config.showSeconds) return@ToggleRow
                    scope.launch { dataStore.updateConfig(config.copy(showHours = it)) }
                }
                ToggleRow(stringResource(R.string.label_show_seconds), config.showSeconds) {
                    if (!it && !config.showHours) return@ToggleRow
                    scope.launch { dataStore.updateConfig(config.copy(showSeconds = it)) }
                }
                ToggleRow(stringResource(R.string.label_show_buttons), config.showButtons) {
                    scope.launch { dataStore.updateConfig(config.copy(showButtons = it)) }
                }
                ToggleRow(stringResource(R.string.label_wake_lock), config.keepScreenOn) {
                    scope.launch { dataStore.updateConfig(config.copy(keepScreenOn = it)) }
                }
                ToggleRow(stringResource(R.string.label_focus_mode), config.focusModeEnabled) { isEnabled ->
                    scope.launch { dataStore.updateConfig(config.copy(focusModeEnabled = isEnabled)) }
                    if (isEnabled && isServiceRunning()) onStartFocusMode()
                }
                ToggleRow(stringResource(R.string.label_beep_enabled), config.isBeepEnabled) {
                    scope.launch { dataStore.updateConfig(config.copy(isBeepEnabled = it)) }
                }
                ToggleRow(stringResource(R.string.label_vibration_enabled), config.isVibrationEnabled) {
                    scope.launch { dataStore.updateConfig(config.copy(isVibrationEnabled = it)) }
                }

                Spacer(Modifier.height(16.dp))

                TimeLimitField(
                    timeLimitSeconds = config.timeLimitSeconds,
                    onConfirm        = { seconds ->
                        scope.launch { dataStore.updateConfig(config.copy(timeLimitSeconds = seconds)) }
                    }
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                Text(
                    text       = stringResource(R.string.section_appearance),
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color      = MaterialTheme.colorScheme.primary,
                    modifier   = Modifier.padding(bottom = 12.dp)
                )

                ThemeSelector(
                    selectedTheme = config.selectedTheme,
                    onChange      = { theme ->
                        scope.launch {
                            val option = KronoThemeOption.entries.find { it.name == theme }
                                ?: KronoThemeOption.AUTO
                            val (bgColor, txtColor) = overlayColorsForTheme(option, systemIsDark)
                            dataStore.updateConfig(
                                config.copy(
                                    selectedTheme   = theme,
                                    backgroundColor = bgColor,
                                    textColor       = txtColor
                                )
                            )
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                ColorRow(
                    label   = stringResource(R.string.label_background_color),
                    color   = Color(config.backgroundColor).copy(alpha = config.bgOpacity),
                    onClick = { showBgPicker = true }
                )
                Spacer(Modifier.height(8.dp))
                ColorRow(
                    label   = stringResource(R.string.label_text_color),
                    color   = Color(config.textColor).copy(alpha = config.textOpacity),
                    onClick = { showTextPicker = true }
                )

                Spacer(Modifier.height(24.dp))

                AppearanceSlider(
                    label    = stringResource(R.string.label_scale),
                    value    = config.scale,
                    minLabel = "0.5x",
                    maxLabel = "1.5x",
                    range    = 0.5f..1.5f,
                    display  = String.format(Locale.US, "%.1fx", config.scale),
                    onChange = { scope.launch { dataStore.updateConfig(config.copy(scale = it)) } }
                )

                Spacer(Modifier.height(12.dp))

                AppearanceSlider(
                    label    = stringResource(R.string.label_corner_radius),
                    value    = config.cornerRadius,
                    minLabel = "0dp",
                    maxLabel = "50dp",
                    range    = 0f..50f,
                    display  = "${config.cornerRadius.toInt()}dp",
                    onChange = { scope.launch { dataStore.updateConfig(config.copy(cornerRadius = it)) } }
                )

                Spacer(Modifier.height(48.dp))
            }

            TextButton(
                onClick  = { showAboutDialog = true },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Text(
                    text       = stringResource(R.string.label_about),
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showBgPicker) {
        ColorPickerDialog(
            title          = stringResource(R.string.label_background_color),
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
            title          = stringResource(R.string.label_text_color),
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



    if (showAboutDialog) {
        AboutDialog(
            onDismiss       = { showAboutDialog = false },
            onSupportClick  = {
                showAboutDialog = false
                context.startActivity(android.content.Intent(context, com.krono.app.ui.DonationActivity::class.java).apply {
                    putExtra("manual_trigger", true)
                })
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
