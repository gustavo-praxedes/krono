package com.krono.app.ui

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

// ============================================================
// ColorPickerDialog.kt
// Correções:
//   • Removida variável "updatingFromSlider" — não era lida
//   • Removidas as atribuições redundantes a ela
//   • LaunchedEffect simplificado para apenas sincronizar os
//     campos de texto quando os sliders mudarem
// ============================================================

@Composable
fun ColorPickerDialog(
    title         : String,
    initialColor  : Color,
    initialOpacity: Float,
    onConfirm     : (color: Color, opacity: Float) -> Unit,
    onDismiss     : () -> Unit,
    onPreview     : (color: Color, opacity: Float) -> Unit = { _, _ -> }
) {
    val initHsv = remember(initialColor) {
        FloatArray(3).also { colorToHSV(initialColor.copy(alpha = 1f).toArgb(), it) }
    }

    var hue     by remember { mutableFloatStateOf(initHsv[0]) }
    var sat     by remember { mutableFloatStateOf(initHsv[1]) }
    var bri     by remember { mutableFloatStateOf(initHsv[2]) }
    var opacity by remember { mutableFloatStateOf(initialOpacity.coerceIn(0f, 1f)) }

    val previewArgb by remember(hue, sat, bri) {
        derivedStateOf { HSVToColor(floatArrayOf(hue, sat, bri)) }
    }
    val previewColor by remember(previewArgb, opacity) {
        derivedStateOf { Color(previewArgb).copy(alpha = opacity) }
    }

    val hexFromSliders by remember(previewArgb) {
        derivedStateOf { "%06X".format(previewArgb and 0xFFFFFF) }
    }
    val rFromSliders by remember(previewArgb) { derivedStateOf { (previewArgb shr 16) and 0xFF } }
    val gFromSliders by remember(previewArgb) { derivedStateOf { (previewArgb shr 8)  and 0xFF } }
    val bFromSliders by remember(previewArgb) { derivedStateOf { previewArgb           and 0xFF } }

    // Estados locais dos campos — independentes dos sliders durante digitação
    var hexText by remember(hexFromSliders) { mutableStateOf(hexFromSliders) }
    var rText   by remember(rFromSliders)   { mutableStateOf(rFromSliders.toString()) }
    var gText   by remember(gFromSliders)   { mutableStateOf(gFromSliders.toString()) }
    var bText   by remember(bFromSliders)   { mutableStateOf(bFromSliders.toString()) }

    // Sincroniza campos de texto quando sliders mudam
    LaunchedEffect(hexFromSliders) {
        hexText = hexFromSliders
        rText   = rFromSliders.toString()
        gText   = gFromSliders.toString()
        bText   = bFromSliders.toString()
    }

    // Preview em tempo real no overlay
    LaunchedEffect(previewColor) {
        onPreview(Color(previewArgb), opacity)
    }

    fun applyHex(input: String) {
        val clean = input.trimStart('#').trim()
        if (clean.length == 6) {
            try {
                val parsed = clean.toLong(16).toInt() or (0xFF shl 24)
                val hsv    = FloatArray(3)
                colorToHSV(parsed, hsv)
                hue = hsv[0]; sat = hsv[1]; bri = hsv[2]
            } catch (_: NumberFormatException) { }
        }
    }

    fun applyRgb(r: String, g: String, b: String) {
        val ri = r.toIntOrNull()?.coerceIn(0, 255) ?: return
        val gi = g.toIntOrNull()?.coerceIn(0, 255) ?: return
        val bi = b.toIntOrNull()?.coerceIn(0, 255) ?: return
        val argb = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
        val hsv  = FloatArray(3)
        colorToHSV(argb, hsv)
        hue = hsv[0]; sat = hsv[1]; bri = hsv[2]
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier       = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape          = RoundedCornerShape(20.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text       = title,
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // ── Preview + Campos HEX / RGB ────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 72.dp, height = 72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    ) {
                        CheckerboardBackground(Modifier.matchParentSize())
                        Box(modifier = Modifier.matchParentSize().background(previewColor))
                    }

                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Campo HEX
                        OutlinedTextField(
                            value         = hexText,
                            onValueChange = { input ->
                                hexText = input
                                    .trimStart('#')
                                    .filter { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }
                                    .take(6)
                                    .uppercase()
                            },
                            label           = { Text("#HEX") },
                            prefix          = { Text("#", fontFamily = FontFamily.Monospace) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType   = KeyboardType.Ascii,
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction      = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { applyHex(hexText) }
                            ),
                            singleLine = true,
                            textStyle  = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize   = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Campos R / G / B
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // R
                            RgbField(
                                label    = "R",
                                value    = rText,
                                onChange = { rText = it },
                                onDone   = { applyRgb(rText, gText, bText) },
                                modifier = Modifier.weight(1f)
                            )
                            // G
                            RgbField(
                                label    = "G",
                                value    = gText,
                                onChange = { gText = it },
                                onDone   = { applyRgb(rText, gText, bText) },
                                modifier = Modifier.weight(1f)
                            )
                            // B
                            RgbField(
                                label    = "B",
                                value    = bText,
                                onChange = { bText = it },
                                onDone   = { applyRgb(rText, gText, bText) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider()

                // ── Slider H ─────────────────────────────────
                val hueGradient = remember {
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green,
                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                }
                HsbSliderRow(
                    label         = "H",
                    value         = hue,
                    displayValue  = "${hue.roundToInt()}°",
                    valueRange    = 0f..360f,
                    trackBrush    = hueGradient,
                    onValueChange = { hue = it }
                )

                // ── Slider S ─────────────────────────────────
                val satBrush by remember(hue, bri) {
                    derivedStateOf {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(HSVToColor(floatArrayOf(hue, 0f, bri))),
                                Color(HSVToColor(floatArrayOf(hue, 1f, bri)))
                            )
                        )
                    }
                }
                HsbSliderRow(
                    label         = "S",
                    value         = sat,
                    displayValue  = "${(sat * 100).roundToInt()}%",
                    valueRange    = 0f..1f,
                    trackBrush    = satBrush,
                    onValueChange = { sat = it }
                )

                // ── Slider B ─────────────────────────────────
                val briBrush by remember(hue, sat) {
                    derivedStateOf {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black,
                                Color(HSVToColor(floatArrayOf(hue, sat, 1f)))
                            )
                        )
                    }
                }
                HsbSliderRow(
                    label         = "B",
                    value         = bri,
                    displayValue  = "${(bri * 100).roundToInt()}%",
                    valueRange    = 0f..1f,
                    trackBrush    = briBrush,
                    onValueChange = { bri = it }
                )

                HorizontalDivider()

                // ── Slider Opacidade ─────────────────────────
                val opacityBrush by remember(previewArgb) {
                    derivedStateOf {
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color(previewArgb))
                        )
                    }
                }
                HsbSliderRow(
                    label         = "Opacidade",
                    value         = opacity,
                    displayValue  = "${(opacity * 100).roundToInt()}%",
                    valueRange    = 0f..1f,
                    trackBrush    = opacityBrush,
                    onValueChange = { opacity = it }
                )

                // ── Botões ───────────────────────────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancelar") }

                    Button(
                        onClick  = { onConfirm(Color(previewArgb), opacity) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Confirmar") }
                }
            }
        }
    }
}

// Componente extraído para evitar lambdas aninhadas em Triple
@Composable
private fun RgbField(
    label    : String,
    value    : String,
    onChange : (String) -> Unit,
    onDone   : () -> Unit,
    modifier : Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = { input ->
            onChange(input.filter { it.isDigit() }.take(3))
        },
        label           = { Text(label) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction    = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        singleLine = true,
        textStyle  = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace,
            fontSize   = 13.sp
        ),
        modifier = modifier
    )
}

@Composable
private fun HsbSliderRow(
    label         : String,
    value         : Float,
    displayValue  : String,
    valueRange    : ClosedFloatingPointRange<Float>,
    trackBrush    : Brush,
    onValueChange : (Float) -> Unit
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = label,
            modifier   = Modifier.width(72.dp),
            style      = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Box(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(trackBrush)
            )
            Slider(
                value         = value,
                onValueChange = onValueChange,
                valueRange    = valueRange,
                modifier      = Modifier.fillMaxWidth(),
                colors        = SliderDefaults.colors(
                    activeTrackColor   = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor         = Color.White
                )
            )
        }
        Text(
            text       = displayValue,
            modifier   = Modifier.width(52.dp),
            style      = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CheckerboardBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.horizontalGradient(
                0.0f to Color(0xFFCCCCCC),
                0.5f to Color(0xFFCCCCCC),
                0.5f to Color(0xFF999999),
                1.0f to Color(0xFF999999)
            )
        )
    )
}