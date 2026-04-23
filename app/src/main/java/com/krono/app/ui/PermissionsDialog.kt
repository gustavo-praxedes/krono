package com.krono.app.ui

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.krono.app.ui.theme.KronoTokens
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsDialog(
    hasNotificationPermission : Boolean,
    hasOverlayPermission      : Boolean,
    hasInstallPermission      : Boolean,
    onRequestNotification     : () -> Unit,
    onRequestOverlay          : () -> Unit,
    onRequestInstall          : () -> Unit,
    onDismiss                 : () -> Unit
) {
    // Overlay só libera com notificação + overlay. Install é opcional.
    val coreGranted = hasOverlayPermission &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotificationPermission)

    // Removido o LaunchedEffect de fechamento automático para o usuário ver o check verde.

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

                // ── Permissão: Notificações (Android 13+) ────
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionItem(
                        icon        = Icons.Default.Notifications,
                        title       = "Notificações",
                        description = "Exibe o cronômetro na barra de status.",
                        granted     = hasNotificationPermission,
                        optional    = false,
                        onClick     = onRequestNotification
                    )
                    Spacer(Modifier.height(KronoTokens.Spacing.md))
                }

                // ── Permissão: Overlay ────────────────────────
                PermissionItem(
                    icon        = Icons.Default.Settings,
                    title       = "Exibir sobre outros apps",
                    description = "Permite flutuar o widget sobre qualquer app.",
                    granted     = hasOverlayPermission,
                    optional    = false,
                    onClick     = onRequestOverlay
                )

                Spacer(Modifier.height(KronoTokens.Spacing.md))

                // ── Permissão: Instalar APK (opcional) ───────
                PermissionItem(
                    icon        = Icons.Default.Download,
                    title       = "Instalar atualizações",
                    description = "Opcional. Permite instalar novas versões direto no app.",
                    granted     = hasInstallPermission,
                    optional    = true,
                    onClick     = onRequestInstall
                )

                Spacer(Modifier.height(KronoTokens.Spacing.sectionGap))

                // ── Botão Concluir (aparece quando core OK) ───
                AnimatedVisibility(
                    visible = coreGranted,
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
    icon       : ImageVector,
    title      : String,
    description: String,
    granted    : Boolean,
    optional   : Boolean,
    onClick    : () -> Unit
) {
    val containerColor = when {
        granted  -> MaterialTheme.colorScheme.primaryContainer
        optional -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        else     -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (granted)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    val checkTint = if (granted) Color(0xFF10B981) else MaterialTheme.colorScheme.outline

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
                tint               = if (granted) Color(0xFF10B981) else contentColor,
                modifier           = Modifier.size(KronoTokens.Icon.dialogHeader)
            )

            Spacer(Modifier.width(KronoTokens.Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = title,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    if (optional) {
                        Spacer(Modifier.width(KronoTokens.Spacing.xs))
                        Text(
                            text  = "opcional",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
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
                tint               = checkTint,
                modifier           = Modifier.size(KronoTokens.Icon.status)
            )
        }
    }
}
