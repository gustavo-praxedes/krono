package com.krono.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.BuildConfig
import com.krono.app.util.UpdateInfo
import com.krono.app.util.UpdateResult
import com.krono.app.util.checkForUpdate
import kotlinx.coroutines.launch

enum class ItemType(val emoji: String) {
    FEAT("✨"), FIX("🐛"), PERF("⚡"), DOCS("📝"), CHORE("🔧"), OTHER("✅")
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
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            shape    = RoundedCornerShape(28.dp),
            color    = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Novidades da Versão", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("v${updateInfo.tagName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(Modifier.height(20.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 350.dp)
                ) {
                    items(changelogItems) { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(item.type.emoji, fontSize = 18.sp, modifier = Modifier.padding(end = 12.dp, top = 2.dp))
                            Text(item.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (checking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Text("Verificando...", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        val result = lastResult
                        val isUpToDate = result is UpdateResult.UpToDate ||
                                (result is UpdateResult.UpdateAvailable && result.info.tagName == BuildConfig.VERSION_NAME)

                        if (isUpToDate) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Já está atualizado!", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Verificar Atualizações")
                            }
                        }
                    }
                }
            }
        }
    }
}