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
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.krono.app.data.parseTimeLimitInput
import com.krono.app.service.MainService
import com.krono.app.ui.theme.KronoTheme
import com.krono.app.ui.theme.KronoThemeOption
import com.krono.app.util.UpdateInfo
import com.krono.app.util.UpdateResult
import com.krono.app.util.checkForUpdate
import com.krono.app.viewmodel.TimerViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val navigationEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)

    // Sinaliza ao Composable para exibir o dialog de permissão de overlay
    private val overlayPermissionDialogEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private lateinit var dataStore: OverlayDataStore
    private val timerViewModel: TimerViewModel
        get() = (application as KronoApp).timerViewModel
    private var pendingUpdateInfo: UpdateInfo? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    // Após retornar das configurações do Android, verifica se a permissão
    // foi concedida e inicia o serviço automaticamente se sim.
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            startServiceAndMinimize()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra("open_settings", false)) {
            navigationEvents.tryEmit(AppRoutes.SETTINGS)
        }
    }

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

    // ========================================================
    // NAVEGAÇÃO
    // ========================================================

    @Composable
    private fun AppContent(showDonationDialog: Boolean = false) {
        val navController = rememberNavController()
        val timerState    by timerViewModel.timerState.collectAsState()

        // Controla o dialog de permissão de overlay — acionado pelo Flow
        var showOverlayPermissionDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            launch {
                navigationEvents.collect { route ->
                    navController.navigate(route) { launchSingleTop = true }
                }
            }
            launch {
                overlayPermissionDialogEvents.collect {
                    showOverlayPermissionDialog = true
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
                        startForegroundService(
                            Intent(this@MainActivity, MainService::class.java)
                                .apply { action = ACTION_RESET }
                        )
                    },
                    onOpenOverlay  = { tryStartService { } },
                    onOpenSettings = { navController.navigate(AppRoutes.SETTINGS) }
                )
            }

            composable(AppRoutes.SETTINGS) {
                SettingsScreen(
                    showDonationDialog = showDonationDialog,
                    onBack             = { navController.popBackStack() }
                )
            }
        }

        // ── Dialog de permissão de overlay ────────────────────
        if (showOverlayPermissionDialog) {
            OverlayPermissionDialog(
                onConfirm = {
                    showOverlayPermissionDialog = false
                    overlayPermissionLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                },
                onDismiss = {
                    showOverlayPermissionDialog = false
                }
            )
        }
    }

    // ========================================================
    // DIALOG DE PERMISSÃO DE OVERLAY
    // ========================================================

    @Composable
    private fun OverlayPermissionDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector        = Icons.Default.Layers,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text       = "Permissão necessária",
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
            },
            text = {
                Text(
                    text  = "Para exibir o cronômetro sobre outros apps, o Krono precisa " +
                            "da permissão \"Exibir sobre outros apps\".\n\n" +
                            "Na próxima tela, encontre o Krono e ative a permissão.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text("Configurar", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Text("Agora não")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ========================================================
    // TELA DE CONFIGURAÇÕES
    // ========================================================

    @Composable
    private fun SettingsScreen(
        showDonationDialog: Boolean = false,
        onBack            : (() -> Unit)? = null
    ) {
        val config       = dataStore.configFlow.collectAsState(initial = OverlayConfig()).value
        val scope        = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current

        var showBgPicker          by remember { mutableStateOf(false) }
        var showTextPicker        by remember { mutableStateOf(false) }
        var showAboutDialog       by remember { mutableStateOf(false) }
        var showDonation          by remember { mutableStateOf(showDonationDialog) }
        var showDonationFromAbout by remember { mutableStateOf(false) }

        var changelogInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        var updateInfo    by remember { mutableStateOf<UpdateInfo?>(pendingUpdateInfo) }

        @OptIn(ExperimentalMaterial3Api::class)
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
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Voltar"
                                )
                            }
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
                            startForegroundService(
                                Intent(this@MainActivity, MainService::class.java)
                                    .apply { action = ACTION_START_FOCUS }
                            )
                        }
                    }
                    ToggleRow("Bipe Ativo", config.isBeepEnabled) {
                        scope.launch { dataStore.updateConfig(config.copy(isBeepEnabled = it)) }
                    }
                    ToggleRow("Vibração Ativa", config.isVibrationEnabled) {
                        scope.launch { dataStore.updateConfig(config.copy(isVibrationEnabled = it)) }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Tempo Limite ──────────────────────────
                    var typedDigits by remember { mutableStateOf("") }

                    LaunchedEffect(config.timeLimitSeconds) {
                        if (typedDigits.isEmpty() && config.timeLimitSeconds > 0) {
                            val hrs  = config.timeLimitSeconds / 3600
                            val mins = (config.timeLimitSeconds % 3600) / 60
                            val secs = config.timeLimitSeconds % 60
                            typedDigits = String.format("%04d%02d%02d", hrs, mins, secs)
                                .toLongOrNull()?.toString() ?: ""
                        }
                    }

                    val limitPadded    = typedDigits.padStart(8, '0')
                    val limitFormatted = "${limitPadded.substring(0, 4)}:${limitPadded.substring(4, 6)}:${limitPadded.substring(6)}"
                    val limitTfValue   = androidx.compose.ui.text.input.TextFieldValue(
                        text      = limitFormatted,
                        selection = androidx.compose.ui.text.TextRange(limitFormatted.length)
                    )

                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text       = "Tempo Limite Máximo",
                                style      = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text  = "HH:MM:SS • 0000:00:00 = ilimitado",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value         = limitTfValue,
                            onValueChange = { input ->
                                val rawDigits = input.text.filter { it.isDigit() }
                                if (rawDigits.length > 8) {
                                    val added     = rawDigits.last()
                                    val base      = if (typedDigits.length < 8) typedDigits else typedDigits.drop(1)
                                    val candidate = base + added
                                    val padded    = candidate.padStart(8, '0')
                                    val cMins     = padded.substring(4, 6).toIntOrNull() ?: 0
                                    val cSecs     = padded.substring(6, 8).toIntOrNull() ?: 0
                                    if (cMins <= 59 && cSecs <= 59) {
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
                            singleLine = true,
                            modifier   = Modifier.width(160.dp),
                            shape      = RoundedCornerShape(12.dp),
                            textStyle  = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                textAlign  = TextAlign.Center,
                                fontSize   = 18.sp
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))

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

        // ── Diálogos ──────────────────────────────────────────

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
                    if (isServiceRunning()) {
                        startForegroundService(
                            Intent(this@MainActivity, MainService::class.java)
                                .apply { action = ACTION_SHOW_OVERLAY }
                        )
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
                onDismiss  = {
                    updateInfo        = null
                    pendingUpdateInfo = null
                }
            )
        }
    }

    // ========================================================
    // COMPONENTES DE UI REUTILIZÁVEIS
    // ========================================================

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ThemeSelector(selectedTheme: String, onChange: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        val current  = KronoThemeOption.entries.find { it.name == selectedTheme }
            ?: KronoThemeOption.AUTO

        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text     = "Tema",
                style    = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            ExposedDropdownMenuBox(
                expanded         = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier         = Modifier.width(200.dp)
            ) {
                OutlinedTextField(
                    value         = current.label,
                    onValueChange = {},
                    readOnly      = true,
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    shape         = RoundedCornerShape(8.dp),
                    modifier      = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    textStyle     = MaterialTheme.typography.bodyMedium,
                    singleLine    = true,
                )

                ExposedDropdownMenu(
                    expanded         = expanded,
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
            Text(
                text     = label,
                style    = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
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
                    value         = value,
                    onValueChange = onChange,
                    valueRange    = range,
                    modifier      = Modifier.weight(1f)
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

    // ========================================================
    // UTILITÁRIOS
    // ========================================================

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == MainService::class.java.name }
    }

    // Inicia o serviço e minimiza o app — chamado após permissão confirmada
    private fun startServiceAndMinimize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        startForegroundService(Intent(this, MainService::class.java))
        moveTaskToBack(true)
    }

    private fun tryStartService(onStarted: () -> Unit) {
        if (!Settings.canDrawOverlays(this)) {
            // Sem permissão → dispara evento para mostrar o dialog explicativo
            overlayPermissionDialogEvents.tryEmit(Unit)
            return
        }
        startServiceAndMinimize()
        onStarted()
    }

    private fun returnToOverlay() {
        startForegroundService(
            Intent(this, MainService::class.java).apply { action = ACTION_SHOW_OVERLAY }
        )
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