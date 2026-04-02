package com.gustavo.cronometro.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gustavo.cronometro.data.OverlayConfig
import com.gustavo.cronometro.data.OverlayDataStore
import com.gustavo.cronometro.service.MainService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontFamily
import com.gustavo.cronometro.data.formatTimeLimitSeconds
import com.gustavo.cronometro.data.parseTimeLimitInput

class MainActivity : ComponentActivity() {

    private lateinit var dataStore: OverlayDataStore

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStore = OverlayDataStore(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }

    // ========================================================
    // TELA PRINCIPAL
    // ========================================================

    @Composable
    private fun SettingsScreen() {

        val config by dataStore.configFlow.collectAsState(initial = OverlayConfig())
        val scope  = rememberCoroutineScope()
        val focusManager = LocalFocusManager.current

        var serviceRunning by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            while (true) {
                serviceRunning = isServiceRunning()
                delay(500)
            }
        }

        var showBgPicker   by remember { mutableStateOf(false) }
        var showTextPicker by remember { mutableStateOf(false) }

        // ── Estado LOCAL do campo de tempo limite ─────────────
        // NÃO usa config.timeLimitHours como chave do remember —
        // isso causava loop de recomposição e resetava o cursor.
        // O estado é iniciado uma única vez com o valor do DataStore
        // e sincronizado apenas no onDone do teclado.
        var limitText by remember {
            mutableStateOf(formatTimeLimitSeconds(config.timeLimitSeconds))
        }

        LaunchedEffect(config.autoLaunch) {
            if (config.autoLaunch && !serviceRunning) {
                tryStartService { serviceRunning = true }
                moveTaskToBack(true)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            Spacer(Modifier.height(10.dp))

            // ── Título ────────────────────────────────────────
            Text(
                text       = "CONFIGURAÇÕES",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 20.dp)
            )

            Spacer(Modifier.height(10.dp))

            // ── Botão Principal ───────────────────────────────
            Button(
                onClick = {
                    if (serviceRunning) {
                        stopService(Intent(this@MainActivity, MainService::class.java))
                        serviceRunning = false
                    } else {
                        tryStartService { serviceRunning = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serviceRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector        = if (serviceRunning) Icons.Default.Stop
                    else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier           = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = if (serviceRunning) "Fechar Cronômetro Flutuante"
                    else "Abrir Cronômetro Flutuante",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Toggles ───────────────────────────────────────

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
            ToggleRow("Bipe Ativo", config.isBeepEnabled) {
                scope.launch { dataStore.updateConfig(config.copy(isBeepEnabled = it)) }
            }
            ToggleRow("Vibração Ativa", config.isVibrationEnabled) {
                scope.launch { dataStore.updateConfig(config.copy(isVibrationEnabled = it)) }
            }


            // ── Campo Tempo Limite ────────────────────────────
            // Formato: HHHH:MM:SS | 0000:00:00 = ilimitado
            // Estado local — não depende do DataStore durante digitação.
            // Salva apenas ao pressionar Done no teclado.
            var limitText by remember {
                mutableStateOf(formatTimeLimitSeconds(config.timeLimitSeconds))
            }

            // Sincroniza quando o DataStore muda externamente
            LaunchedEffect(config.timeLimitSeconds) {
                limitText = formatTimeLimitSeconds(config.timeLimitSeconds)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "Tempo Limite Máximo",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text  = "HHHH:MM:SS  •  0000:00:00 = ilimitado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value         = limitText,
                    onValueChange = { input ->
                        // Aceita apenas dígitos e ":" — formata automaticamente
                        val digits = input.filter { it.isDigit() }.take(8)
                        // Reconstrói no formato HHHH:MM:SS ao digitar
                        limitText = when {
                            digits.length <= 4 -> digits
                            digits.length <= 6 ->
                                "${digits.substring(0, 4)}:${digits.substring(4)}"
                            else ->
                                "${digits.substring(0, 4)}:${digits.substring(4, 6)}:${digits.substring(6)}"
                        }
                    },
                    placeholder = {
                        Text(
                            text      = "0000:00:00",
                            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier  = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val seconds = parseTimeLimitInput(limitText) ?: 0L
                            limitText = formatTimeLimitSeconds(seconds)

                            scope.launch {
                                dataStore.updateConfig(
                                    config.copy(timeLimitSeconds = seconds)
                                )
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    singleLine  = true,
                    modifier = Modifier
                        .width(160.dp)
                        .height(64.dp),
                    shape       = RoundedCornerShape(8.dp),
                    textStyle     = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center, // Centralização Horizontal
                        fontSize   = 18.sp,
                        // REMOVE O ESPAÇO EXTRA DA FONTE (Centralização Vertical Perfeita)
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                            includeFontPadding = false
                        )
                    )
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            // ── Seção APARÊNCIA ───────────────────────────────
            Text(
                text       = "APARÊNCIA",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
                modifier   = Modifier.padding(bottom = 16.dp)
            )

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

            // ── Sliders Escala e Cantos ───────────────────────
            // Column com fillMaxWidth garante comprimentos idênticos
            // e alinhamento consistente com os ToggleRows acima.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Escala: 0.5x – 2.0x
                AppearanceSlider(
                    label    = "Escala",
                    value    = config.scale,
                    minLabel = "0.5x",
                    maxLabel = "1.5x",
                    range    = 0.5f..1.5f,
                    display  = "${String.format("%.1f", config.scale)}x",
                    onChange = {
                        scope.launch { dataStore.updateConfig(config.copy(scale = it)) }
                    }
                )

                // Cantos: 0dp – 50dp (máximo restringido a 50)
                AppearanceSlider(
                    label    = "Cantos",
                    value    = config.cornerRadius,
                    minLabel = "0dp",
                    maxLabel = "50dp",
                    range    = 0f..50f,
                    display  = "${config.cornerRadius.toInt()}px",
                    onChange = {
                        scope.launch { dataStore.updateConfig(config.copy(cornerRadius = it)) }
                    }
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        // ── Diálogos de Cor ───────────────────────────────────

        if (showBgPicker) {
            ColorPickerDialog(
                title          = "Cor de Fundo",
                initialColor   = Color(config.backgroundColor),
                initialOpacity = config.bgOpacity,
                onPreview = { color, opacity ->
                    scope.launch {
                        dataStore.updateConfig(
                            config.copy(
                                backgroundColor = color.toArgb(),
                                bgOpacity       = opacity
                            )
                        )
                    }
                },
                onConfirm = { color, opacity ->
                    scope.launch {
                        dataStore.updateConfig(
                            config.copy(
                                backgroundColor = color.toArgb(),
                                bgOpacity       = opacity
                            )
                        )
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
                onPreview = { color, opacity ->
                    scope.launch {
                        dataStore.updateConfig(
                            config.copy(
                                textColor   = color.toArgb(),
                                textOpacity = opacity
                            )
                        )
                    }
                },
                onConfirm = { color, opacity ->
                    scope.launch {
                        dataStore.updateConfig(
                            config.copy(
                                textColor   = color.toArgb(),
                                textOpacity = opacity
                            )
                        )
                    }
                    showTextPicker = false
                },
                onDismiss = { showTextPicker = false }
            )
        }
    }

    // ========================================================
    // COMPONENTES REUTILIZÁVEIS
    // ========================================================

    @Composable
    private fun ToggleRow(
        label    : String,
        checked  : Boolean,
        onChange : (Boolean) -> Unit
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
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
    private fun ColorRow(
        label   : String,
        color   : Color,
        onClick : () -> Unit
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
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

    // Slider de aparência com label, min/max e valor atual.
    // Usa fillMaxWidth para alinhar com os ToggleRows.
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
            // Linha de label + valor atual
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text       = display,
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
            // Linha do slider com labels min/max
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

    private fun tryStartService(onStarted: () -> Unit) {
        if (!Settings.canDrawOverlays(this)) {
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
        startForegroundService(Intent(this, MainService::class.java))
        onStarted()
    }
}