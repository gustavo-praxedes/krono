package com.krono.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.util.ApkInstaller
import com.krono.app.util.UpdateInfo

// ============================================================
// UpdateDialog.kt
//
// Exibe changelog da nova versão e gerencia o download/instalação.
//
// Estados do download:
//   Idle → Downloading (progress 0..1) → Installing → Done/Error
// ============================================================

private enum class DownloadState { IDLE, DOWNLOADING, INSTALLING, ERROR }

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss : () -> Unit
) {
    val context        = LocalContext.current
    val changelogItems = remember(updateInfo.changelog) { parseChangelog(updateInfo.changelog) }

    var state    by remember { mutableStateOf(DownloadState.IDLE) }
    var progress by remember { mutableFloatStateOf(0f) }
    var errorMsg by remember { mutableStateOf("") }

    val downloadUrl = updateInfo.downloadUrl ?: updateInfo.releaseUrl

    Dialog(
        onDismissRequest = { if (state != DownloadState.DOWNLOADING) onDismiss() },
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier       = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f),
            shape          = RoundedCornerShape(28.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Cabeçalho ─────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text       = "Atualização Disponível",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.align(Alignment.Center)
                    )
                    // X só disponível se não estiver baixando
                    if (state != DownloadState.DOWNLOADING) {
                        IconButton(
                            onClick  = onDismiss,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Badge nova versão ─────────────────────────
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text       = "Versão ${updateInfo.tagName}  •  nova",
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary,
                        modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // ── Changelog da nova versão ──────────────────
                if (changelogItems.isEmpty()) {
                    Text(
                        text      = "Nenhuma nota disponível.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(changelogItems) { item ->
                            ChangelogItem(item = item)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Área de progresso / erro ──────────────────
                when (state) {
                    DownloadState.DOWNLOADING -> {
                        Text(
                            text  = "Baixando... ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    DownloadState.INSTALLING -> {
                        Text(
                            text  = "Instalando...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(16.dp))
                    }
                    DownloadState.ERROR -> {
                        Text(
                            text  = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    DownloadState.IDLE -> { }
                }

                // ── Botão principal ───────────────────────────
                Button(
                    onClick  = {
                        state    = DownloadState.DOWNLOADING
                        progress = 0f
                        ApkInstaller.downloadAndInstall(
                            context     = context,
                            downloadUrl = downloadUrl,
                            version     = updateInfo.tagName,
                            onProgress  = { p ->
                                progress = p
                                if (p >= 1f) state = DownloadState.INSTALLING
                            },
                            onError     = { msg ->
                                errorMsg = msg
                                state    = DownloadState.ERROR
                            }
                        )
                    },
                    enabled  = state == DownloadState.IDLE || state == DownloadState.ERROR,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Download,
                        contentDescription = null,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = if (state == DownloadState.ERROR) "Tentar Novamente"
                        else "Baixar e Instalar",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}