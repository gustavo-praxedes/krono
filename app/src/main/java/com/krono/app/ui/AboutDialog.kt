package com.krono.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.BuildConfig
import com.krono.app.R
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
            context.resources.openRawResource(R.raw.changelog)
                .bufferedReader().readText()
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
        properties       = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier       = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape          = RoundedCornerShape(24.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Botão fechar alinhado ao topo ──────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick  = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd).size(36.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

//                // ── Ícone + identidade ─────────────────────────
//                Box(
//                    modifier        = Modifier
//                        .size(72.dp)
//                        .clip(RoundedCornerShape(20.dp))
//                        .background(MaterialTheme.colorScheme.primaryContainer),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Icon(
//                        imageVector        = Icons.Default.Timer,
//                        contentDescription = null,
//                        tint               = MaterialTheme.colorScheme.primary,
//                        modifier           = Modifier.size(40.dp)
//                    )
//                }


                Text(
                    text       = "Krono",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

//                Text(
//                    text  = "Cronômetro flutuante para Android",
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )

//                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(20.dp))

                // ── Descrição com hierarquia ───────────────────
                Text(
                    text = "O Krono é um App independente " +
                            "desenvolvido para quem precisa medir o tempo " +
                            "enquanto usa outros aplicativos.\n\n" +
                            "O widget fica visível sobre qualquer tela, " +
                            "pode ser movido livremente e personalizado " +
                            "com cores, transparência e tamanho.\n\n" +
                            "Este projeto é gratuito, sem anúncios e " +
                            "de código aberto. Se ele tem sido útil para " +
                            "você, considere apoiar o desenvolvimento para " +
                            "que ele continue evoluindo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

//                Spacer(Modifier.height(8.dp))
//
//                Text(
//                    text  = "Gratuito, sem anúncios e de código aberto.",
//                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = FontWeight.Medium,
//                    color = MaterialTheme.colorScheme.onSurface,
//                    textAlign = TextAlign.Center
//                )

                Spacer(Modifier.height(24.dp))

                // ── Botão principal ────────────────────────────
                Button(
                    onClick  = onSupportClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "Apoiar o Projeto",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ── Botão secundário ───────────────────────────
                OutlinedButton(
                    onClick  = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "Ver no GitHub",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(12.dp))

                // ── Versão clicável com seta indicando ação ───
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onShowChangelog(localUpdateInfo) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text       = "Versão ${BuildConfig.VERSION_NAME}",
                            style      = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text  = "Ver novidades desta versão",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector        = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}