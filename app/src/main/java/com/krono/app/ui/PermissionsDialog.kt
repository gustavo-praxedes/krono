package com.krono.app.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.krono.app.ui.theme.KronoTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsDialog(
    hasNotificationPermission : Boolean,
    hasOverlayPermission      : Boolean,
    onRequestNotification     : () -> Unit,
    onRequestOverlay          : () -> Unit,
    onDismiss                 : () -> Unit
) {
    val allGranted = hasNotificationPermission &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotificationPermission) &&
            hasOverlayPermission

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier         = Modifier.fillMaxWidth(KronoTokens.Spacing.dialogWidthFrac)
    ) {
        Surface(
            modifier       = Modifier.fillMaxWidth(),
            shape          = KronoTokens.Shape.dialog,
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = KronoTokens.Elevation.dialog
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(KronoTokens.Spacing.dialogPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Cabeçalho ────────────────────────────────
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "Permissões Necessárias",
                        style      = MaterialTheme.typography.headlineSmall.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        fontWeight = FontWeight.Bold,
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

                // ── Descrição ─────────────────────────────────
                Text(
                    text      = "Ative as permissões abaixo para usar todos os recursos do Krono.",
                    style     = MaterialTheme.typography.bodyMedium.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    fontSize  = KronoTokens.Typography.bodyText,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(KronoTokens.Spacing.sectionGap))

                // ── Itens de permissão ────────────────────────
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionItem(
                        icon        = Icons.Default.Notifications,
                        title       = "Notificações",
                        description = "Exibe o cronômetro na barra de status.",
                        granted     = hasNotificationPermission,
                        onClick     = onRequestNotification
                    )

                    Spacer(Modifier.height(KronoTokens.Spacing.md))
                }

                PermissionItem(
                    icon        = Icons.Default.Settings,
                    title       = "Exibir sobre outros apps",
                    description = "Permite flutuar o widget sobre qualquer app.",
                    granted     = hasOverlayPermission,
                    onClick     = onRequestOverlay
                )

                Spacer(Modifier.height(KronoTokens.Spacing.sectionGap))

                // ── Botão Concluir ────────────────────────────
                AnimatedVisibility(
                    visible = allGranted,
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    Button(
                        onClick  = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(KronoTokens.Button.height),
                        shape = KronoTokens.Shape.button
                    ) {
                        Icon(
                            imageVector        = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier           = Modifier.size(KronoTokens.Icon.button)
                        )
                        Spacer(Modifier.width(KronoTokens.Button.iconSpacing))
                        Text(
                            text       = "Concluir",
                            fontSize   = KronoTokens.Typography.buttonLabel,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon       : androidx.compose.ui.graphics.vector.ImageVector,
    title      : String,
    description: String,
    granted    : Boolean,
    onClick    : () -> Unit
) {
    val containerColor = if (granted)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val contentColor = if (granted)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick        = { if (!granted) onClick() },
        enabled        = !granted,
        shape          = KronoTokens.Shape.card,
        color          = containerColor,
        tonalElevation = KronoTokens.Elevation.flat,
        modifier       = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(KronoTokens.Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = if (granted) MaterialTheme.colorScheme.primary else contentColor,
                modifier           = Modifier.size(KronoTokens.Icon.dialogHeader)
            )

            Spacer(Modifier.width(KronoTokens.Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = title,
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }

            Spacer(Modifier.width(KronoTokens.Spacing.md))

            Icon(
                imageVector        = if (granted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint               = if (granted) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                modifier           = Modifier.size(KronoTokens.Icon.status)
            )
        }
    }
}