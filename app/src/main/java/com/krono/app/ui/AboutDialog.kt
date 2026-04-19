package com.krono.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.BuildConfig

// ============================================================
// AboutDialog.kt
// Diálogo "Sobre o App" acessado pelo botão na tela principal.
//
// Callbacks:
//   onDismiss      → fecha o diálogo
//   onSupportClick → fecha o About e abre o DonationDialog
// ============================================================

private const val GITHUB_URL =
    "https://github.com/gustavo-praxedes/cronometro-flutuante"

@Composable
fun AboutDialog(
    onDismiss      : () -> Unit,
    onSupportClick : () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier       = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape          = RoundedCornerShape(28.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier            = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── Cabeçalho com botão X ─────────────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text       = "Sobre o App",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Fechar"
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Texto descritivo ──────────────────────────
                Text(
                    text = "O Cronômetro Flutuante é um app independente " +
                            "desenvolvido para quem precisa medir o tempo " +
                            "enquanto usa outros aplicativos.\n\n" +
                            "O widget fica visível sobre qualquer tela, " +
                            "pode ser movido livremente e personalizado " +
                            "com cores, transparência e tamanho.\n\n" +
                            "Este projeto é gratuito, sem anúncios e " +
                            "de código aberto. Se ele tem sido útil para " +
                            "você, considere apoiar o desenvolvimento para " +
                            "que ele continue evoluindo.",
                    style     = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    color     = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // ── Botões empilhados verticalmente ───────────
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botão Apoiar — abre o DonationDialog
                    Button(
                        onClick  = onSupportClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text       = "Apoiar o Projeto",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Botão GitHub — fundo preto, texto branco
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape  = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor   = Color.White
                        )
                    ) {
                        Text(
                            text       = "Ver no GitHub",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Versão do app ─────────────────────────────
                Text(
                    text      = "Versão ${BuildConfig.VERSION_NAME}",
                    style     = MaterialTheme.typography.labelMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }
}