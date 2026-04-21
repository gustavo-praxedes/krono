package com.krono.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog

// ============================================================
// OverlayPermissionDialog.kt
// Explica ao usuário por que a permissão "Exibir sobre outros
// apps" é necessária antes de abrir as configurações do Android.
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlayPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth() // Faz o diálogo tentar ocupar a largura total
            .padding(horizontal = 16.dp) // Margem mínima nas laterais para não colar no vidro
    ) {
        Surface(
            modifier       = Modifier.fillMaxWidth(), // Garante que o card ocupe tudo o que o diálogo permitir
            shape          = RoundedCornerShape(28.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Cabeçalho ────────────────────────
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = "Permissão Necessária",
                        style    = MaterialTheme.typography.headlineSmall.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
                    )

                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Mensagem ─────────────────────────
                Text(
                    text = buildAnnotatedString {
                        append("Para exibir o cronômetro sobre outros apps, o ")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            append("Krono")
                        }
                        append(" precisa da permissão \"Exibir sobre outros apps\".\n\nNa próxima tela, encontre o app e ative a permissão.")
                    },
                    style      = MaterialTheme.typography.bodyLarge.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    fontSize   = 16.sp,
                    textAlign  = TextAlign.Center,
                    lineHeight = 22.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(24.dp))

                // ── Botão ────────────────────────────
                Button(
                    onClick  = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor   = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text       = "Configurar",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}