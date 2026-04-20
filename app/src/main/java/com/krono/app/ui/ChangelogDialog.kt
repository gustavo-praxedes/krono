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
    updateInfo       : UpdateInfo,
    onDismiss        : () -> Unit,
    onUpdateAvailable: (UpdateInfo) -> Unit
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
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 1. Spacer com o mesmo tamanho do IconButton para equilibrar o centro
                    Spacer(modifier = Modifier.size(36.dp))

                    // 2. Coluna centralizada usando weight(1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally // Centraliza os textos entre si
                    ) {
                        Text(
                            text       = "Novidades da Versão",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.onSurface,
                            textAlign  = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text  = "v${updateInfo.tagName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            textAlign  = TextAlign.Center
                        )
                    }

                    // 3. O botão de fechar
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

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))

                // ── Lista de mudanças ─────────────────────────
                if (changelogItems.isEmpty()) {
                    Box(
                        modifier         = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text      = "Nenhuma nota disponível para esta versão.",
                            style     = MaterialTheme.typography.bodyMedium,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding      = PaddingValues(vertical = 2.dp)
                    ) {
                        items(changelogItems) { item ->
                            ChangelogItem(item = item)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(12.dp))

                // ── Feedback inline com animação ──────────────
                AnimatedVisibility(
                    visible = noUpdateMsg || errorMsg != null,
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        color    = if (noUpdateMsg)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text     = if (noUpdateMsg) "✓  Você já tem a versão mais recente."
                            else errorMsg ?: "",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = if (noUpdateMsg) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }

                // ── Botão verificar — outline, hierarquia secundária ──
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
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    if (checking) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Verificando...",
                            fontSize = 14.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Icon(
                            imageVector        = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier           = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Verificar Atualizações",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Botão fechar — filled, ação primária ──────
                Button(
                    onClick  = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text("Fechar", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Item do changelog — minimalista, sem ruído de cor ────────
@Composable
internal fun ChangelogItem(item: ChangelogListItem) {
    val accentColor = when (item.type) {
        ItemType.FEAT  -> MaterialTheme.colorScheme.primary
        ItemType.FIX   -> MaterialTheme.colorScheme.error
        ItemType.PERF  -> MaterialTheme.colorScheme.tertiary
        ItemType.DOCS  -> MaterialTheme.colorScheme.onSurfaceVariant
        ItemType.CHORE -> MaterialTheme.colorScheme.onSurfaceVariant
        ItemType.OTHER -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val typeLabel = when (item.type) {
        ItemType.FEAT  -> "novo"
        ItemType.FIX   -> "fix"
        ItemType.PERF  -> "perf"
        ItemType.DOCS  -> "docs"
        ItemType.CHORE -> "maint"
        ItemType.OTHER -> null
    }

    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Ícone colorido por tipo
        Icon(
            imageVector        = Icons.Default.CheckCircle,
            contentDescription = null,
            tint               = accentColor.copy(alpha = 0.8f),
            modifier           = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
            if (typeLabel != null) {
                Text(
                    text  = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
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
        if (clean.startsWith("#")) continue

        val type = when {
            clean.contains("feat") || clean.contains("✨") -> ItemType.FEAT
            clean.contains("fix")  || clean.contains("🐛") -> ItemType.FIX
            clean.contains("perf") || clean.contains("⚡") -> ItemType.PERF
            clean.contains("docs") || clean.contains("📝") -> ItemType.DOCS
            clean.contains("chore")|| clean.contains("🔧") -> ItemType.CHORE
            else -> ItemType.OTHER
        }

        val text = clean
            .replace(Regex("^(feat|fix|perf|docs|chore|refactor)(\\(.+\\))?:\\s*"), "")
            .replace(Regex("^[✨🐛⚡📝🔧✅💄]\\s*"), "")
            .trim()
            .let { it.replaceFirstChar { c -> c.uppercase() } }

        if (text.isNotBlank()) items.add(ChangelogListItem(text, type))
    }

    return items.ifEmpty { listOf(ChangelogListItem(changelog.take(300), ItemType.OTHER)) }
}