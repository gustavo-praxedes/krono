package com.krono.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.data.OverlayDataStore
import com.krono.app.data.formatLifetimeDetailed
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle

// ============================================================
// DonationDialog.kt
// Diálogo de doação exibido após 12h de uso acumulado.
//
// Elementos:
//   • Botão X para fechar no canto superior direito
//   • Mensagem com tempo total formatado em HHh MMm SSs
//   • Botão "Doar via Pix" — copia chave e exibe Toast
//   • Botão "Doar com Cartão / Ko-fi" — abre link no navegador
// ============================================================

// ── Configurações de doação ───────────────────────────────────
// Substitua pelos valores reais antes de publicar
private const val KOFI_URL = "https://ko-fi.com/gustavopraxedes"

@Composable
fun DonationDialog(
    onDismiss: () -> Unit, // Acionado pelo botão X
    onDonate : () -> Unit  // Acionado pelos botões de Pix/Cartão
) {
    val context   = LocalContext.current
    val dataStore = remember { OverlayDataStore(context) }
    val config    by dataStore.configFlow.collectAsState(
        initial = com.krono.app.data.OverlayConfig()
    )

    val totalLifetimeMs = config.totalLifetimeMs
    val formattedTime   = formatLifetimeDetailed(totalLifetimeMs)

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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Cabeçalho ────────────────────────────────
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = "Apoie o Projeto",
                        style    = MaterialTheme.typography.headlineSmall.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 22.sp
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

                // Espaço real de 20dp (compensado pela remoção do font padding)
                Spacer(Modifier.height(20.dp))

                // ── Mensagem ─────────────────────────────────
                Text(
                    text = buildAnnotatedString {
                        append("Incrível! Você já utilizou nosso Cronômetro por ")
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            append(formattedTime)
                        }
                        append(". Este projeto é independente e seu apoio ajuda a mantê-lo gratuito e sem anúncios.")
                    },
                    style     = MaterialTheme.typography.bodyLarge.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    fontSize  = 16.sp,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(20.dp))

                // ── Botões de Doação (Chamam onDonate) ─────────
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botão Unificado: Ko-fi / Cartão com aparência de "Apoiar"
                    Button(
                        onClick = {
                            openKofi(context)
                            onDonate()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape    = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,    // Cor sólida para destaque
                            contentColor   = MaterialTheme.colorScheme.onPrimary  // Texto em contraste
                        )
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Coffee,
                            contentDescription = null,
                            modifier           = Modifier.size(20.dp)
                        )

                        Spacer(Modifier.width(10.dp)) // Espaçamento entre ícone e texto

                        Text(
                            text       = "Pagar um café",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ── Abre o link do Ko-fi no navegador ────────────────────────
private fun openKofi(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(KOFI_URL))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}