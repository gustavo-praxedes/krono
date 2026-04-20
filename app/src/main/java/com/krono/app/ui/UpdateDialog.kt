package com.krono.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.util.ApkInstaller
import com.krono.app.util.DownloadStatus
import com.krono.app.util.UpdateInfo

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss : () -> Unit
) {
    val context = LocalContext.current
    val changelogItems = remember(updateInfo.changelog) { parseChangelog(updateInfo.changelog) }
    val version = updateInfo.tagName.removePrefix("v")

    var downloadStatus by remember { mutableStateOf(ApkInstaller.getDownloadStatus(context)) }
    var downloadId by remember { mutableStateOf(-1L) }
    var showDownloadStartedMsg by remember { mutableStateOf(false) }

    val isDownloaded = downloadStatus is DownloadStatus.Completed
    val isDownloading = downloadStatus is DownloadStatus.Downloading
    val progress = (downloadStatus as? DownloadStatus.Downloading)?.percent?.toFloat()?.div(100f) ?: 0f

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
            kotlinx.coroutines.delay(3000)
            showDownloadStartedMsg = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss, // Sempre permite fechar ao clicar fora
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true, // Sempre permite fechar com o botão voltar
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Nova versão",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Botão de fechar agora permanece visível mesmo durante o download
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "v$version disponível",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 36.dp).align(Alignment.Start)
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f, fill = false)) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(changelogItems) { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = item.type.icon,
                                    contentDescription = null,
                                    tint = item.type.iconTint,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(text = item.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = showDownloadStartedMsg,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "O download continuará em segundo plano!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (isDownloading) {
                    Spacer(Modifier.height(8.dp))
                    Column {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        Text(
                            text = "Baixando: ${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                        )
                    }
                }

                if (downloadStatus is DownloadStatus.Failed) {
                    Text(
                        text = (downloadStatus as DownloadStatus.Failed).error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isDownloaded) {
                            ApkInstaller.installApk(context, version) //
                        } else if (!isDownloading) {
                            val url = updateInfo.downloadUrl ?: return@Button //
                            downloadId = ApkInstaller.startDownload(context, url, version) //
                            showDownloadStartedMsg = true
                        }
                    },
                    enabled = !isDownloading || isDownloaded,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    when {
                        isDownloaded -> {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Instalar agora", fontWeight = FontWeight.Bold)
                        }
                        isDownloading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Baixando...")
                        }
                        else -> {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Baixar e instalar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}