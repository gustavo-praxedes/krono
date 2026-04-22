package com.krono.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.BuildConfig
import com.krono.app.R
import com.krono.app.ui.theme.KronoTokens
import com.krono.app.util.UpdateInfo

private const val GITHUB_URL = "https://github.com/gustavo-praxedes/krono"

@Composable
fun AboutDialog(
    onDismiss      : () -> Unit,
    onSupportClick : () -> Unit,
    onShowChangelog: (UpdateInfo) -> Unit
) {
    val context = LocalContext.current

    val localChangelog = remember {
        try {
            context.resources.openRawResource(R.raw.changelog).bufferedReader().readText()
        } catch (_: Exception) { "" }
    }

    val localUpdateInfo = remember {
        UpdateInfo(
            tagName     = BuildConfig.VERSION_NAME,
            changelog   = localChangelog,
            releaseUrl  = GITHUB_URL,
            downloadUrl = null
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
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
                        text       = "Krono",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize   = KronoTokens.Typography.dialogTitle
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

                // ── Descrição ─────────────────────────────────
                Text(
                    text      = "O widget que flutua sobre qualquer app. Gratuito, sem anúncios e de código aberto.",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    fontSize  = KronoTokens.Typography.bodyText
                )

                Spacer(Modifier.height(KronoTokens.Spacing.sectionGap))

                // ── Botões ────────────────────────────────────
                Button(
                    onClick  = onSupportClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KronoTokens.Button.height),
                    shape  = KronoTokens.Shape.button
                ) {
                    Icon(
                        imageVector        = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier           = Modifier.size(KronoTokens.Icon.button)
                    )
                    Spacer(Modifier.width(KronoTokens.Button.iconSpacing))
                    Text(
                        text       = "Apoiar",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = KronoTokens.Typography.buttonLabel
                    )
                }

                Spacer(Modifier.height(KronoTokens.Spacing.sm))

                OutlinedButton(
                    onClick  = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KronoTokens.Button.height),
                    shape = KronoTokens.Shape.button
                ) {
                    Icon(
                        imageVector        = Icons.Default.Code,
                        contentDescription = null,
                        modifier           = Modifier.size(KronoTokens.Icon.button)
                    )
                    Spacer(Modifier.width(KronoTokens.Button.iconSpacing))
                    Text(
                        text       = "Código Fonte",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = KronoTokens.Typography.buttonLabel
                    )
                }

                Spacer(Modifier.height(KronoTokens.Spacing.sectionGap))

                // ── Versão / Changelog ────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowChangelog(localUpdateInfo) }
                        .padding(vertical = KronoTokens.Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Versão ${BuildConfig.VERSION_NAME}",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text  = "Ver Novidades",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(KronoTokens.Icon.dialogHeader)
                    )
                }
            }
        }
    }
}