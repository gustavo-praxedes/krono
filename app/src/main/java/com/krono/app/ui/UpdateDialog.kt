package com.krono.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
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

    val downloadUrl    = updateInfo.downloadUrl ?: updateInfo.releaseUrl
    val isDownloading  = state == DownloadState.DOWNLOADING
    val isInstalling   = state == DownloadState.INSTALLING
    val isBusy         = isDownloading || isInstalling

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier       = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f),
            shape          = RoundedCornerShape(24.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // ── Cabeçalho ─────────────────────────────────
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.NewReleases,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Nova Versão",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text  = "v${updateInfo.tagName} disponível",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!isBusy) {
                        IconButton(
                            onClick  = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint               = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))

                // ── Changelog da nova versão ──────────────────
                if (changelogItems.isEmpty()) {
                    Box(
                        modifier         = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text      = "Nenhuma nota disponível.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(changelogItems) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // CORREÇÃO: Usando o Emoji como Texto em vez do ícone de Check
                                Text(
                                    text = item.type.emoji,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))

                // ── Área de progresso ─────────────────────────
                when (state) {
                    DownloadState.DOWNLOADING -> {
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text  = "Baixando atualização...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text       = "${(progress * 100).toInt()}%",
                                style      = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            trackColor    = MaterialTheme.colorScheme.surfaceVariant,
                            color         = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    DownloadState.INSTALLING -> {
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text  = "Abrindo instalador...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    DownloadState.ERROR -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            color    = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text     = errorMsg,
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
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
                    enabled  = !isBusy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    when {
                        isBusy -> {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isDownloading) "Baixando ${(progress * 100).toInt()}%..."
                                else "Instalando...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        state == DownloadState.ERROR -> {
                            Icon(
                                imageVector        = Icons.Default.Download,
                                contentDescription = null,
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Tentar Novamente",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        else -> {
                            Icon(
                                imageVector        = Icons.Default.Download,
                                contentDescription = null,
                                modifier           = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Baixar e Instalar",
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}