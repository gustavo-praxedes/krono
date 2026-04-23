package com.krono.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.ui.theme.KronoTokens
import com.krono.app.util.ApkInstaller
import com.krono.app.util.DownloadStatus
import com.krono.app.util.UpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss : () -> Unit
) {
    val context = LocalContext.current
    val version = remember(updateInfo.tagName) { updateInfo.tagName.removePrefix("v") }

    // Processa o changelog usando a mesma lógica amigável do ChangelogDialog
    val changelogItems = remember(updateInfo.changelog) {
        val items = parseChangelog(updateInfo.changelog)
        if (items.isEmpty()) {
            listOf(ChangelogItem("Esta atualização traz melhorias de estabilidade e correções internas.", ItemType.OTHER))
        } else {
            items
        }
    }

    var downloadStatus by remember {
        val initialStatus = if (ApkInstaller.getDownloadedFile(context, version)?.exists() == true) {
            DownloadStatus.Completed
        } else {
            ApkInstaller.getDownloadStatus(context)
        }
        mutableStateOf(initialStatus)
    }

    var downloadId             by remember { mutableLongStateOf(-1L) }
    var showDownloadStartedMsg by remember { mutableStateOf(false) }

    val isDownloaded  = downloadStatus is DownloadStatus.Completed
    val isDownloading = downloadStatus is DownloadStatus.Downloading
    val progress      = (downloadStatus as? DownloadStatus.Downloading)?.percent?.toFloat()?.div(100f) ?: 0f

    LaunchedEffect(downloadId) {
        if (downloadId != -1L) {
            while (true) {
                kotlinx.coroutines.delay(500)
                downloadStatus = ApkInstaller.getDownloadStatus(context)
                if (downloadStatus is DownloadStatus.Completed || downloadStatus is DownloadStatus.Failed) break
            }
        }
    }

    LaunchedEffect(showDownloadStartedMsg) {
        if (showDownloadStartedMsg) {
            kotlinx.coroutines.delay(KronoTokens.Animation.toastDurationMs.toLong())
            showDownloadStartedMsg = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = true
        )
    ) {
        Surface(
            modifier       = Modifier
                .fillMaxWidth(KronoTokens.Spacing.dialogWidthFrac)
                .wrapContentHeight(),
            shape          = KronoTokens.Shape.dialog,
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = KronoTokens.Elevation.dialog
        ) {
            Column(
                modifier            = Modifier.padding(KronoTokens.Spacing.dialogPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Cabeçalho ────────────────────────────────
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = "Nova Versão Disponível",
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = KronoTokens.Typography.dialogTitle,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.padding(horizontal = 40.dp)
                        )
                        Text(
                            text     = "Versão v$version",
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier
                            .size(KronoTokens.Icon.close)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(KronoTokens.Spacing.xl))

                // ── Notificação de Download Iniciado ──────────
                AnimatedVisibility(
                    visible = showDownloadStartedMsg,
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    Card(
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = KronoTokens.Spacing.md)
                    ) {
                        Row(
                            modifier          = Modifier.padding(KronoTokens.Spacing.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Download,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier           = Modifier.size(KronoTokens.Icon.status)
                            )
                            Spacer(Modifier.width(KronoTokens.Spacing.sm))
                            Text(
                                text  = "O download continuará em segundo plano!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // ── Progresso do Download ─────────────────────
                if (isDownloading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = KronoTokens.Spacing.lg)
                    ) {
                        LinearProgressIndicator(
                            progress  = { progress },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .height(KronoTokens.Stroke.progressBar),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap  = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Row(
                            modifier              = Modifier
                                .fillMaxWidth()
                                .padding(top = KronoTokens.Spacing.xs),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text  = "Baixando: ${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Erro de Download ──────────────────────────
                if (downloadStatus is DownloadStatus.Failed) {
                    Text(
                        text     = "Falha no download. Tente novamente.",
                        color    = MaterialTheme.colorScheme.error,
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = KronoTokens.Spacing.sm)
                    )
                }

                // ── Botão de Ação Principal ───────────────────
                Button(
                    onClick = {
                        if (isDownloaded) {
                            ApkInstaller.installApk(context, version)
                        } else if (!isDownloading) {
                            val url = updateInfo.downloadUrl ?: return@Button
                            downloadId = ApkInstaller.startDownload(context, url, version)
                            showDownloadStartedMsg = true
                        }
                    },
                    enabled  = !isDownloading || isDownloaded,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KronoTokens.Button.height),
                    shape = KronoTokens.Shape.button
                ) {
                    when {
                        isDownloaded -> {
                            Icon(
                                imageVector        = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier           = Modifier.size(KronoTokens.Icon.button)
                            )
                            Spacer(Modifier.width(KronoTokens.Button.iconSpacing))
                            Text(
                                text       = "Instalar agora",
                                fontWeight = FontWeight.Bold,
                                fontSize   = KronoTokens.Typography.buttonLabel
                            )
                        }
                        isDownloading -> {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(KronoTokens.Component.buttonSpinner),
                                strokeWidth = KronoTokens.Stroke.circularIndicator,
                                color       = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(KronoTokens.Button.iconSpacing))
                            Text(
                                text     = "Baixando...",
                                fontSize = KronoTokens.Typography.buttonLabel
                            )
                        }
                        else -> {
                            Icon(
                                imageVector        = Icons.Default.Download,
                                contentDescription = null,
                                modifier           = Modifier.size(KronoTokens.Icon.button)
                            )
                            Spacer(Modifier.width(KronoTokens.Button.iconSpacing))
                            Text(
                                text       = "Baixar e instalar",
                                fontWeight = FontWeight.Bold,
                                fontSize   = KronoTokens.Typography.buttonLabel
                            )
                        }
                    }
                }
            }
        }
    }
}
