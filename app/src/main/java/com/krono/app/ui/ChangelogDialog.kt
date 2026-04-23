package com.krono.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.BuildConfig
import com.krono.app.ui.theme.KronoTokens
import com.krono.app.util.UpdateInfo
import com.krono.app.util.UpdateResult
import com.krono.app.util.checkForUpdate
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

enum class ItemType(val icon: ImageVector, val iconTint: androidx.compose.ui.graphics.Color) {
    FEAT (Icons.Default.AutoAwesome,          androidx.compose.ui.graphics.Color(0xFF10B981)),
    FIX  (Icons.Default.BugReport,            androidx.compose.ui.graphics.Color(0xFFEF4444)),
    PERF (Icons.Default.Speed,                androidx.compose.ui.graphics.Color(0xFFF59E0B)),
    DOCS (Icons.AutoMirrored.Filled.Article,  androidx.compose.ui.graphics.Color(0xFF8B5CF6)),
    CHORE(Icons.Default.Build,                androidx.compose.ui.graphics.Color(0xFF6B7280)),
    OTHER(Icons.Default.Check,                androidx.compose.ui.graphics.Color(0xFF3B82F6))
}

data class ChangelogItem(val text: String, val type: ItemType)

/**
 * Converte o markdown do changelog em uma lista de objetos categorizados.
 * Suporta detecção por cabeçalho (#) ou por prefixo de item (feat:, fix:, etc).
 */
fun parseChangelog(changelog: String): List<ChangelogItem> {
    if (changelog.isBlank()) return emptyList()
    val items = mutableListOf<ChangelogItem>()
    var sectionType = ItemType.OTHER

    changelog.lines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isBlank()) return@forEach

        // 1. Detecção por cabeçalho de seção (Markdown #)
        if (trimmed.startsWith("#")) {
            sectionType = when {
                trimmed.contains("Novidades", true)    || trimmed.contains("✨") -> ItemType.FEAT
                trimmed.contains("Correções", true)    || trimmed.contains("🐛") -> ItemType.FIX
                trimmed.contains("Performance", true)  || trimmed.contains("⚡") -> ItemType.PERF
                trimmed.contains("Documentação", true) || trimmed.contains("📝") -> ItemType.DOCS
                trimmed.contains("Manutenção", true)   || trimmed.contains("🔧") -> ItemType.CHORE
                else -> sectionType
            }
            return@forEach
        }

        // 2. Processamento de itens da lista (-, *, •)
        if (trimmed.startsWith("-") || trimmed.startsWith("*") || trimmed.startsWith("•")) {
            // Remove o marcador de lista
            val content = trimmed.substring(1).trim()
                .replace(Regex("\\[.*?\\]\\(.*?\\)"), "") // Remove links markdown

            if (content.isBlank() || content.contains("Comparação completa", true)) return@forEach

            // Detecta o tipo pelo prefixo (feat:, fix:, etc)
            val itemType = when {
                content.startsWith("feat",  true) -> ItemType.FEAT
                content.startsWith("fix",   true) -> ItemType.FIX
                content.startsWith("perf",  true) -> ItemType.PERF
                content.startsWith("docs",  true) -> ItemType.DOCS
                content.startsWith("chore", true) -> ItemType.CHORE
                content.startsWith("build", true) -> ItemType.CHORE
                content.startsWith("ci",    true) -> ItemType.CHORE
                else -> sectionType // Fallback para o tipo da seção atual
            }

            // Limpa o prefixo do texto final (ex: "feat: novo timer" -> "novo timer")
            val finalText = if (content.contains(":")) {
                content.substringAfter(":").trim()
            } else {
                content
            }.replaceFirstChar { it.uppercase() }

            items.add(ChangelogItem(finalText, itemType))
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
        properties       = DialogProperties(usePlatformDefaultWidth = false)
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
                    Text(
                        text       = "Novidades da Versão ${updateInfo.tagName}",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = KronoTokens.Typography.dialogTitle,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.padding(horizontal = 40.dp)
                    )

                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier
                            .size(KronoTokens.Icon.close)
                            .align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(KronoTokens.Spacing.sectionGap))

                // ── Lista de itens ────────────────────────────
                Box(modifier = Modifier.weight(1f, fill = false)) {
                    LazyColumn(
                        modifier            = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(KronoTokens.Spacing.listItemGap),
                        horizontalAlignment = Alignment.Start
                    ) {
                        items(changelogItems) { item ->
                            Row(
                                modifier          = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector        = item.type.icon,
                                    contentDescription = null,
                                    tint               = item.type.iconTint,
                                    modifier           = Modifier
                                        .size(KronoTokens.Icon.listItem)
                                        .padding(top = KronoTokens.Spacing.xs)
                                )

                                Spacer(Modifier.width(KronoTokens.Spacing.listIconGap))

                                Text(
                                    text     = item.text,
                                    style    = MaterialTheme.typography.bodyMedium.copy(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(KronoTokens.Spacing.sectionGap))

                // ── Botão / Status ────────────────────────────
                AnimatedVisibility(
                    visible = !checking,
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    val result     = lastResult
                    val isUpToDate = result is UpdateResult.UpToDate ||
                            (result is UpdateResult.UpdateAvailable &&
                                    result.info.tagName == BuildConfig.VERSION_NAME)

                    if (isUpToDate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.primary,
                                modifier           = Modifier.size(KronoTokens.Icon.status)
                            )
                            Spacer(Modifier.width(KronoTokens.Spacing.sm))
                            Text(
                                text  = "Atualizado",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    checking = true
                                    val response = checkForUpdate(BuildConfig.VERSION_NAME)
                                    lastResult = response
                                    checking = false
                                    if (response is UpdateResult.UpdateAvailable &&
                                        response.info.tagName != BuildConfig.VERSION_NAME
                                    ) {
                                        onUpdateAvailable(response.info)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(KronoTokens.Button.height),
                            shape  = KronoTokens.Shape.button,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor   = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier           = Modifier.size(KronoTokens.Icon.button)
                            )
                            Spacer(Modifier.width(KronoTokens.Button.iconSpacing))
                            Text(
                                text       = "Verificar Atualizações",
                                fontSize   = KronoTokens.Typography.buttonLabel,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ── Spinner de verificação ────────────────────
                if (checking) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(KronoTokens.Component.inlineSpinner),
                            strokeWidth = KronoTokens.Stroke.circularIndicator
                        )
                        Spacer(Modifier.width(KronoTokens.Spacing.md))
                        Text(
                            text  = "Verificando...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
