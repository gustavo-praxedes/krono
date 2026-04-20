package com.krono.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.BuildConfig
import com.krono.app.util.UpdateInfo
import com.krono.app.util.UpdateResult
import com.krono.app.util.checkForUpdate
import kotlinx.coroutines.launch

@Composable
fun ChangelogDialog(
    updateInfo        : UpdateInfo,
    onDismiss         : () -> Unit,
    onUpdateAvailable : (UpdateInfo) -> Unit
) {
    val scope          = rememberCoroutineScope()
    val changelogItems = remember(updateInfo.changelog) { parseChangelog(updateInfo.changelog) }

    var checking    by remember { mutableStateOf(false) }
    var noUpdateMsg by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier       = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.80f),
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
                        text       = "Novidades da Versão",
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }


                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                // ── Lista de mudanças ─────────────────────────
                if (changelogItems.isEmpty()) {
                    Box(
                        modifier            = Modifier.weight(1f),
                        contentAlignment    = Alignment.Center
                    ) {
                        Text(
                            text      = "Nenhuma nota de lançamento disponível.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
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

                // ── Feedback de estado ────────────────────────
                when {
                    noUpdateMsg -> Text(
                        text  = "✓ Você já tem a versão mais recente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    errorMsg != null -> Text(
                        text  = errorMsg!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(8.dp))

                // ── Verificar Atualizações ────────────────────
                OutlinedButton(
                    onClick  = {
                        checking    = true
                        noUpdateMsg = false
                        errorMsg    = null
                        scope.launch {
                            when (val result = checkForUpdate(BuildConfig.VERSION_NAME)) {
                                is UpdateResult.UpdateAvailable -> {
                                    checking = false
                                    onUpdateAvailable(result.info)
                                }
                                is UpdateResult.UpToDate -> {
                                    checking    = false
                                    noUpdateMsg = true
                                }
                                else -> {
                                    checking = false
                                    errorMsg  = "Sem conexão. Tente novamente."
                                }
                            }
                        }
                    },
                    enabled  = !checking,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    if (checking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Verificando...")
                    } else {
                        Icon(
                            imageVector        = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Verificar Atualizações",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Text("Fechar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun ChangelogItem(item: ChangelogListItem) {
    val bgColor = when (item.type) {
        ItemType.FEAT  -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ItemType.FIX   -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ItemType.PERF  -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ItemType.DOCS  -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ItemType.CHORE -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ItemType.OTHER -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
    }
    val iconColor = when (item.type) {
        ItemType.FEAT  -> MaterialTheme.colorScheme.primary
        ItemType.FIX   -> MaterialTheme.colorScheme.error
        ItemType.PERF  -> MaterialTheme.colorScheme.tertiary
        else           -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = bgColor,
        shape    = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier              = Modifier.padding(12.dp),
            verticalAlignment     = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = null,
                tint               = iconColor,
                modifier           = Modifier.size(20.dp)
            )
            Text(
                text     = item.text,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Tipos e parser ────────────────────────────────────────────

enum class ItemType { FEAT, FIX, PERF, DOCS, CHORE, OTHER }

data class ChangelogListItem(val text: String, val type: ItemType)

fun parseChangelog(changelog: String): List<ChangelogListItem> {
    if (changelog.isBlank()) return emptyList()

    val items = mutableListOf<ChangelogListItem>()

    for (line in changelog.split("\n")) {
        val trimmed = line.trim()
        if (trimmed.isBlank()) continue

        val clean = trimmed
            .removePrefix("- ").removePrefix("* ").removePrefix("• ").trim()

        if (clean.isBlank()) continue
        if (clean.startsWith("**Full Changelog**")) continue
        if (clean.startsWith("http") || clean.startsWith("[")) continue
        if (clean.startsWith("#")) continue  // ignora headers markdown

        val type = when {
            clean.contains("feat") || clean.contains("✨") -> ItemType.FEAT
            clean.contains("fix")  || clean.contains("🐛") -> ItemType.FIX
            clean.contains("perf") || clean.contains("⚡") -> ItemType.PERF
            clean.contains("docs") || clean.contains("📝") -> ItemType.DOCS
            clean.contains("chore")|| clean.contains("🔧") -> ItemType.CHORE
            else -> ItemType.OTHER
        }

        val text = clean
            .replace(Regex("^(feat|fix|perf|docs|chore)(\\(.+\\))?:\\s*"), "")
            .replace(Regex("^[✨🐛⚡📝🔧✅💄]\\s*"), "")
            .trim()

        if (text.isNotBlank()) items.add(ChangelogListItem(text, type))
    }

    return items.ifEmpty { listOf(ChangelogListItem(changelog.take(300), ItemType.OTHER)) }
}