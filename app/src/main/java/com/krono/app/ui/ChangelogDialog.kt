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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.BuildConfig
import com.krono.app.util.UpdateInfo
import com.krono.app.util.UpdateResult
import com.krono.app.util.checkForUpdate
import kotlinx.coroutines.launch

enum class ItemType(val icon: ImageVector, val iconTint: androidx.compose.ui.graphics.Color) {
    FEAT(Icons.Default.AutoAwesome, androidx.compose.ui.graphics.Color(0xFF10B981)),
    FIX(Icons.Default.BugReport, androidx.compose.ui.graphics.Color(0xFFEF4444)),
    PERF(Icons.Default.Speed, androidx.compose.ui.graphics.Color(0xFFF59E0B)),
    DOCS(Icons.Default.Article, androidx.compose.ui.graphics.Color(0xFF8B5CF6)),
    CHORE(Icons.Default.Build, androidx.compose.ui.graphics.Color(0xFF6B7280)),
    OTHER(Icons.Default.Check, androidx.compose.ui.graphics.Color(0xFF3B82F6))
}

data class ChangelogItem(val text: String, val type: ItemType)

fun parseChangelog(changelog: String): List<ChangelogItem> {
    if (changelog.isBlank()) return emptyList()
    val items = mutableListOf<ChangelogItem>()
    var currentType = ItemType.OTHER

    changelog.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) return@forEach

        if (trimmed.startsWith("#")) {
            currentType = when {
                trimmed.contains("Novidades") || trimmed.contains("✨") -> ItemType.FEAT
                trimmed.contains("Correções") || trimmed.contains("🐛") -> ItemType.FIX
                trimmed.contains("Performance") || trimmed.contains("⚡") -> ItemType.PERF
                trimmed.contains("Documentação") || trimmed.contains("📝") -> ItemType.DOCS
                trimmed.contains("Manutenção") || trimmed.contains("🔧") -> ItemType.CHORE
                else -> currentType
            }
            return@forEach
        }

        if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•")) {
            val cleanText = trimmed
                .removePrefix("-").removePrefix("*").removePrefix("•")
                .replace(Regex("\\[.*?\\]\\(.*?\\)"), "")
                .trim()

            if (cleanText.isNotBlank() && !cleanText.contains("Comparação completa")) {
                items.add(ChangelogItem(cleanText, currentType))
            }
        }
    }
    return items
}

@Composable
fun ChangelogDialog(
    updateInfo       : UpdateInfo,
    onDismiss        : () -> Unit,
    onUpdateAvailable: (UpdateInfo) -> Unit
) {
    val scope          = rememberCoroutineScope()
    val changelogItems = remember(updateInfo.changelog) { parseChangelog(updateInfo.changelog) }

    var checking   by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<UpdateResult?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape    = RoundedCornerShape(24.dp),
            color    = MaterialTheme.colorScheme.surface,
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
                    Text(
                        "Versão ${updateInfo.tagName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(changelogItems) { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                item.type.icon,
                                contentDescription = null,
                                tint = item.type.iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(item.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = !checking,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val result = lastResult
                    val isUpToDate = result is UpdateResult.UpToDate ||
                            (result is UpdateResult.UpdateAvailable && result.info.tagName == BuildConfig.VERSION_NAME)

                    if (isUpToDate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Atualizado", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    checking = true
                                    val response = checkForUpdate(BuildConfig.VERSION_NAME)
                                    lastResult = response
                                    checking = false

                                    if (response is UpdateResult.UpdateAvailable && response.info.tagName != BuildConfig.VERSION_NAME) {
                                        onUpdateAvailable(response.info)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Verificar atualizações")
                        }
                    }
                }

                if (checking) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Verificando...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}