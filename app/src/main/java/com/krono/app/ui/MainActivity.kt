package com.krono.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.krono.app.ACTION_RESET
import com.krono.app.ACTION_SHOW_OVERLAY
import com.krono.app.ACTION_START_FOCUS
import com.krono.app.BuildConfig
import com.krono.app.EXTRA_SHOW_DONATION
import com.krono.app.KronoApp
import com.krono.app.data.OverlayConfig
import com.krono.app.data.OverlayDataStore
import com.krono.app.data.formatTimeLimitSeconds
import com.krono.app.data.parseTimeLimitInput
import com.krono.app.service.MainService
import com.krono.app.ui.theme.KronoTheme
import com.krono.app.ui.theme.KronoThemeOption
import com.krono.app.util.UpdateInfo
import com.krono.app.util.UpdateResult
import com.krono.app.util.checkForUpdate
import com.krono.app.viewmodel.TimerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val navigationEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    private lateinit var dataStore: OverlayDataStore

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("open_settings", false)) {
            navigationEvents.tryEmit(AppRoutes.SETTINGS)
        }
    }
    private val timerViewModel: TimerViewModel
        get() = (application as KronoApp).timerViewModel
    private var pendingUpdateInfo: UpdateInfo? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStore = OverlayDataStore(this)

        if (intent?.getBooleanExtra("open_settings", false) == true) {
            navigationEvents.tryEmit(AppRoutes.SETTINGS)
        }

        val showDonation = intent.getBooleanExtra(EXTRA_SHOW_DONATION, false)

        onBackPressedDispatcher.addCallback(this) {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
        }

        setContent {
            val config by dataStore.configFlow.collectAsState(initial = OverlayConfig())

            // GIT 7: KronoTheme substitui MaterialTheme direto
            KronoTheme(selectedTheme = config.selectedTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    AppContent(showDonationDialog = showDonation)
                }
            }
        }

        checkForUpdateIfNeeded { info ->
            pendingUpdateInfo = info
        }
    }

    @Composable
    private fun AppContent(showDonationDialog: Boolean = false) {
        val navController = rememberNavController()
        val timerState    by timerViewModel.timerState.collectAsState()
        val config        by dataStore.configFlow.collectAsState(initial = OverlayConfig())

        LaunchedEffect(Unit) {
            launch {
                navigationEvents.collect { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }
            }
            val cfg = dataStore.configFlow.first()
            if (cfg.autoLaunch && !isTaskRoot) {
                tryStartService { }
                moveTaskToBack(true)
            }
        }

        NavHost(
            navController    = navController,
            startDestination = AppRoutes.TIMER
        ) {
            composable(AppRoutes.TIMER) {
                TimerScreen(
                    timerState     = timerState,
                    onStart        = { timerViewModel.start() },
                    onPause        = { timerViewModel.pause() },
                    onReset        = {
                        val intent = Intent(
                            this@MainActivity,
                            MainService::class.java
                        ).apply { action = ACTION_RESET }
                        startForegroundService(intent)
                    },
                    onOpenOverlay  = {
                        tryStartService { }
                        moveTaskToBack(true)
                    },
                    onOpenSettings = {
                        navController.navigate(AppRoutes.SETTINGS)
                    }
                )
            }

            composable(AppRoutes.SETTINGS) {
                SettingsScreen(
                    showDonationDialog = showDonationDialog,
                    onBack             = { navController.popBackStack() }
                )
            }
        }
    }

    @Composable
    private fun SettingsScreen(
        showDonationDialog : Boolean = false,
        onBack             : (() -> Unit)? = null
    ) {
        val config = dataStore.configFlow.collectAsState(initial = OverlayConfig()).value
        val scope  = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current

        var showBgPicker          by remember { mutableStateOf(false) }
        var showTextPicker        by remember { mutableStateOf(false) }
        var showAboutDialog       by remember { mutableStateOf(false) }
        var showDonation          by remember { mutableStateOf(showDonationDialog) }
        var showDonationFromAbout by remember { mutableStateOf(false) }
        var showUpdateDialog      by remember { mutableStateOf(pendingUpdateInfo != null) }
        var updateInfo            by remember { mutableStateOf<UpdateInfo?>(pendingUpdateInfo) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Spacer(Modifier.height(5.dp))

                Text(
                    text       = "CONFIGURAÇÕES",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 25.sp,
                    modifier   = Modifier.padding(bottom = 20.dp)
                )

                Spacer(Modifier.height(20.dp))

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
                    if (isEnabled && isServiceRunning()) {
                        val intent = Intent(this@MainActivity, MainService::class.java).apply {
                            action = ACTION_START_FOCUS
                        }
                        startForegroundService(intent)
                    }
                }
                ToggleRow("Bipe Ativo", config.isBeepEnabled) {
                    scope.launch { dataStore.updateConfig(config.copy(isBeepEnabled = it)) }
                }
                ToggleRow("Vibração Ativa", config.isVibrationEnabled) {
                    scope.launch { dataStore.updateConfig(config.copy(isVibrationEnabled = it)) }
                }

                // ── Tempo Limite ──────────────────────────────────
                var typedDigits by remember { mutableStateOf("") }
                
                LaunchedEffect(config.timeLimitSeconds) {
                    if (typedDigits.isEmpty() && config.timeLimitSeconds > 0) {
                        val hrs = config.timeLimitSeconds / 3600
                        val mins = (config.timeLimitSeconds % 3600) / 60
                        val secs = config.timeLimitSeconds % 60
                        typedDigits = String.format("%04d%02d%02d", hrs, mins, secs).toLongOrNull()?.toString() ?: ""
                    }
                }

                val limitPadded = typedDigits.padStart(8, '0')
                val limitFormatted = "${limitPadded.substring(0, 4)}:${limitPadded.substring(4, 6)}:${limitPadded.substring(6)}"
                
                val limitTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(
                    text = limitFormatted,
                    selection = androidx.compose.ui.text.TextRange(limitFormatted.length)
                )

                Row(
                    modifier              = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Tempo Limite Máximo", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text  = "HH:MM:SS • 0000:00:00 = ilimitado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value         = limitTextFieldValue,
                        onValueChange = { input ->
                            val rawDigits = input.text.filter { it.isDigit() }
                            if (rawDigits.length > 8) {
                                val added = rawDigits.last()
                                val base = if (typedDigits.length < 8) typedDigits else typedDigits.drop(1)
                                val candidate = base + added
                                val candidatePadded = candidate.padStart(8, '0')
                                val candidateMins = candidatePadded.substring(4, 6).toIntOrNull() ?: 0
                                val candidateSecs = candidatePadded.substring(6, 8).toIntOrNull() ?: 0
                                
                                if (candidateMins <= 59 && candidateSecs <= 59) {
                                    typedDigits = candidate.toLongOrNull()?.toString() ?: ""
                                }
                            } else if (rawDigits.length < 8) {
                                if (typedDigits.isNotEmpty()) typedDigits = typedDigits.dropLast(1)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            val seconds = parseTimeLimitInput(limitFormatted) ?: 0L
                            scope.launch {
                                dataStore.updateConfig(config.copy(timeLimitSeconds = seconds))
                                focusManager.clearFocus()
                            }
                        }),
                        singleLine  = true,
                        modifier    = Modifier.width(160.dp),
                        shape       = RoundedCornerShape(8.dp),
                        textStyle   = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            textAlign  = TextAlign.Center,
                            fontSize   = 18.sp
                        )
                    )
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(20.dp))

                Text(
                    text     = "APARÊNCIA",
                    style    = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // ── Seletor de Tema — primeiro item da seção ──────
                ThemeSelector(
                    selectedTheme = config.selectedTheme,
                    onChange      = { theme ->
                        scope.launch { dataStore.updateConfig(config.copy(selectedTheme = theme)) }
                    }
                )

                Spacer(Modifier.height(16.dp))

                ColorRow(
                    label   = "Cor de Fundo",
                    color   = Color(config.backgroundColor).copy(alpha = config.bgOpacity),
                    onClick = { showBgPicker = true }
                )
                Spacer(Modifier.height(12.dp))
                ColorRow(
                    label   = "Cor do Texto",
                    color   = Color(config.textColor).copy(alpha = config.textOpacity),
                    onClick = { showTextPicker = true }
                )

                Spacer(Modifier.height(20.dp))

                AppearanceSlider(
                    label    = "Escala",
                    value    = config.scale,
                    minLabel = "0.5x",
                    maxLabel = "1.5x",
                    range    = 0.5f..1.5f,
                    display  = "${String.format("%.1f", config.scale)}x",
                    onChange = { scope.launch { dataStore.updateConfig(config.copy(scale = it)) } }
                )

                AppearanceSlider(
                    label    = "Cantos",
                    value    = config.cornerRadius,
                    minLabel = "0dp",
                    maxLabel = "50dp",
                    range    = 0f..50f,
                    display  = "${config.cornerRadius.toInt()}dp",
                    onChange = { scope.launch { dataStore.updateConfig(config.copy(cornerRadius = it)) } }
                )

                Spacer(Modifier.height(32.dp))
            }

            TextButton(
                onClick  = { showAboutDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text      = "Sobre o App",
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(32.dp))
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

        if (showDonation) {
            DonationDialog(
                onDismiss = {
                    showDonation = false
                    if (isServiceRunning()) {
                        val intent = Intent(this@MainActivity, MainService::class.java).apply {
                            action = ACTION_SHOW_OVERLAY
                        }
                        startForegroundService(intent)
                    }
                },
                onDonate = {
                    showDonation = false
                    returnToOverlay()
                }
            )
        }

        if (showDonationFromAbout) {
            DonationDialog(
                onDismiss = { showDonationFromAbout = false },
                onDonate  = { showDonationFromAbout = false }
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

        if (showAboutDialog) {
            AboutDialog(
                onDismiss      = { showAboutDialog = false },
                onSupportClick = {
                    showAboutDialog       = false
                    showDonationFromAbout = true
                }
            )
        }

        if (showUpdateDialog && updateInfo != null) {
            UpdateDialog(
                updateInfo = updateInfo!!,
                onDismiss  = {
                    showUpdateDialog  = false
                    updateInfo        = null
                    pendingUpdateInfo = null
                }
            )
        }
    }

    // ── Seletor de tema com ExposedDropdownMenuBox ────────────
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ThemeSelector(selectedTheme: String, onChange: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        val current = KronoThemeOption.entries.find { it.name == selectedTheme }
            ?: KronoThemeOption.AUTO

        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Tema", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))

            ExposedDropdownMenuBox(
                expanded        = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier        = Modifier.width(200.dp)
            ) {
                OutlinedTextField(
                    value            = current.label,
                    onValueChange    = {},
                    readOnly         = true,
                    trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape            = RoundedCornerShape(8.dp),
                    modifier         = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    textStyle        = MaterialTheme.typography.bodyMedium,
                    singleLine       = true,
                )

                ExposedDropdownMenu(
                    expanded        = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    KronoThemeOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text    = { Text(option.label) },
                            onClick = {
                                onChange(option.name)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }

    @Composable
    private fun ColorRow(label: String, color: Color, onClick: () -> Unit) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
            )
        }
    }

    @Composable
    private fun AppearanceSlider(
        label    : String,
        value    : Float,
        minLabel : String,
        maxLabel : String,
        range    : ClosedFloatingPointRange<Float>,
        display  : String,
        onChange : (Float) -> Unit
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text       = display,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = minLabel,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Slider(
                    value        = value,
                    onValueChange = onChange,
                    valueRange   = range,
                    modifier     = Modifier.weight(1f)
                )
                Text(
                    text     = maxLabel,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == MainService::class.java.name }
    }

    private fun tryStartService(onStarted: () -> Unit) {
        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        startForegroundService(Intent(this, MainService::class.java))
        onStarted()
    }

    private fun returnToOverlay() {
        val intent = Intent(this, MainService::class.java).apply { action = ACTION_SHOW_OVERLAY }
        startForegroundService(intent)
        finish()
    }

    private fun checkForUpdateIfNeeded(onUpdateAvailable: (UpdateInfo) -> Unit) {
        lifecycleScope.launch {
            val config  = dataStore.configFlow.first()
            val now     = System.currentTimeMillis()
            val elapsed = now - config.lastUpdateCheck
            val oneDay  = 24 * 60 * 60 * 1000L
            if (elapsed < oneDay) return@launch
            dataStore.saveLastUpdateCheck(now)
            val result = checkForUpdate(BuildConfig.VERSION_NAME)
            if (result is UpdateResult.UpdateAvailable) {
                onUpdateAvailable(result.info)
            }
        }
    }
}