package com.krono.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ============================================================
// OverlayPermissionDialog.kt
// Explica ao usuário por que a permissão "Exibir sobre outros
// apps" é necessária antes de abrir as configurações do Android.
// ============================================================

@Composable
fun OverlayPermissionDialog(
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
