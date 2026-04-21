package com.krono.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center // Centraliza o conteúdo principal (o texto)
                ) {
                    Text(
                        text = "Krono",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterEnd) // Alinha o botão especificamente à direita
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "O widget que flutua sobre qualquer app. " +
                    "Gratuito, sem anúncios e de código aberto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onSupportClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Apoiar",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 16.sp,)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Code, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Código fonte",
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 16.sp,)
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowChangelog(localUpdateInfo) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically // Isso garante que o ícone centralize em relação à Column inteira
                ) {
                    Column(
                        modifier = Modifier.weight(1f) // Garante que o texto ocupe o espaço e o ícone fique no canto
                    ) {
                        Text(
                            "Versão ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Ver novidades",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp) // Tamanho aumentado conforme solicitado
                    )
                }
            }
        }
    }
}