package com.krono.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.krono.app.data.parseTimeLimitInput
import com.krono.app.ui.theme.KronoTokens

@Composable
internal fun TimeLimitField(
    timeLimitSeconds: Long,
    onConfirm       : (Long) -> Unit
) {
    val focusManager = LocalFocusManager.current

    var typedDigits by remember { mutableStateOf("") }

    LaunchedEffect(timeLimitSeconds) {
        if (typedDigits.isEmpty() && timeLimitSeconds > 0) {
            val hrs  = timeLimitSeconds / 3600
            val mins = (timeLimitSeconds % 3600) / 60
            val secs = timeLimitSeconds % 60
            typedDigits = String.format("%04d%02d%02d", hrs, mins, secs)
                .toLongOrNull()?.toString() ?: ""
        }
    }

    val limitPadded    = typedDigits.padStart(8, '0')
    val limitFormatted = "${limitPadded.substring(0, 4)}:${limitPadded.substring(4, 6)}:${limitPadded.substring(6)}"
    val tfValue        = TextFieldValue(
        text      = limitFormatted,
        selection = TextRange(limitFormatted.length)
    )

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = KronoTokens.Spacing.md),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Tempo Limite Máximo",
                style      = MaterialTheme.typography.bodyLarge
            )
            Text(
                text  = "0000:00:00 = ilimitado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value         = tfValue,
            onValueChange = { input ->
                val rawDigits = input.text.filter { it.isDigit() }
                when {
                    rawDigits.length > 8 -> {
                        val added     = rawDigits.last()
                        val base      = if (typedDigits.length < 8) typedDigits
                        else typedDigits.drop(1)
                        val candidate = base + added
                        val padded    = candidate.padStart(8, '0')
                        val cMins     = padded.substring(4, 6).toIntOrNull() ?: 0
                        val cSecs     = padded.substring(6, 8).toIntOrNull() ?: 0
                        if (cMins <= 59 && cSecs <= 59) {
                            typedDigits = candidate.toLongOrNull()?.toString() ?: ""
                        }
                    }
                    rawDigits.length < 8 -> {
                        if (typedDigits.isNotEmpty()) typedDigits = typedDigits.dropLast(1)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction    = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                val seconds = parseTimeLimitInput(limitFormatted) ?: 0L
                onConfirm(seconds)
                focusManager.clearFocus()
            }),
            singleLine = true,
            modifier   = Modifier.width(160.dp),
            shape      = KronoTokens.Shape.input,
            textStyle  = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                textAlign  = TextAlign.Center,
                fontSize   = 18.sp
            )
        )
    }
}