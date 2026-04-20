package com.krono.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.krono.app.ui.theme.KronoThemeOption

// ============================================================
// SettingsComponents.kt
// Componentes reutilizáveis da tela de configurações.
// Extraídos da MainActivity para permitir reuso e testabilidade.
// ============================================================

@Composable
internal fun ToggleRow(
    label   : String,
    checked : Boolean,
    onChange: (Boolean) -> Unit
) {
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
internal fun ColorRow(
    label  : String,
    color  : Color,
    onClick: () -> Unit
) {
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
internal fun AppearanceSlider(
    label   : String,
    value   : Float,
    minLabel: String,
    maxLabel: String,
    range   : ClosedFloatingPointRange<Float>,
    display : String,
    onChange: (Float) -> Unit
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeSelector(
    selectedTheme: String,
    onChange     : (String) -> Unit
) {
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
