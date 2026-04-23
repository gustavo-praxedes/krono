package com.krono.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.krono.app.BuildConfig
import com.krono.app.ui.theme.KronoTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// ── Configuração do Google Forms ─────────────────────────────
private const val FORM_ID       = "1FAIpQLSeH4jyM_-SGY_Qsj4NBGSBUhfQIOVjA3L9yhgvtil4QCykyEA"
private const val ENTRY_NAME    = "entry.1606415010"
private const val ENTRY_EMAIL   = "entry.809277435"
private const val ENTRY_MESSAGE = "entry.1665262073"
private const val ENTRY_VERSION = "entry.400223559"
// ─────────────────────────────────────────────────────────────

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

private sealed class SubmitState {
    data object Idle      : SubmitState()
    data object Loading   : SubmitState()
    data object Success   : SubmitState()
    data object Error     : SubmitState()
}

@Composable
fun BugReportDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()

    var name        by remember { mutableStateOf("") }
    var email       by remember { mutableStateOf("") }
    var message     by remember { mutableStateOf("") }
    var submitState by remember { mutableStateOf<SubmitState>(SubmitState.Idle) }

    val emailError  = email.isNotBlank() && !EMAIL_REGEX.matches(email)
    val canSubmit   = message.isNotBlank() &&
            !emailError &&
            submitState is SubmitState.Idle

    // Fecha automaticamente após sucesso (delay para o usuário ver o check)
    LaunchedEffect(submitState) {
        if (submitState is SubmitState.Success) {
            delay(2000)
            onDismiss()
        }
    }

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
                modifier = Modifier
                    .padding(KronoTokens.Spacing.dialogPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Cabeçalho ────────────────────────────────
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "Relatar Bug",
                        style      = MaterialTheme.typography.headlineSmall.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize   = KronoTokens.Typography.dialogTitle,
                        textAlign  = TextAlign.Center
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
                    text      = "Descreva o problema encontrado. Nome e email são opcionais.",
                    style     = MaterialTheme.typography.bodyMedium,
                    fontSize  = KronoTokens.Typography.bodyText,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(KronoTokens.Spacing.sectionGap))

                // ── Nome ──────────────────────────────────────
                OutlinedTextField(
                    value         = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label         = { Text("Nome (opcional)") },
                    singleLine    = true,
                    shape         = KronoTokens.Shape.input,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    supportingText = {
                        Text(
                            text     = "${name.length}/50",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                Spacer(Modifier.height(KronoTokens.Spacing.md))

                // ── Email ─────────────────────────────────────
                OutlinedTextField(
                    value         = email,
                    onValueChange = { if (it.length <= 50) email = it },
                    label         = { Text("Email (opcional)") },
                    singleLine    = true,
                    isError       = emailError,
                    shape         = KronoTokens.Shape.input,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (emailError) "Email inválido" else "", color = MaterialTheme.colorScheme.error)
                            Text("${email.length}/50")
                        }
                    }
                )

                Spacer(Modifier.height(KronoTokens.Spacing.md))

                // ── Mensagem ──────────────────────────────────
                OutlinedTextField(
                    value         = message,
                    onValueChange = { if (it.length <= 250) message = it },
                    label         = { Text("Descrição do problema *") },
                    minLines      = 4,
                    maxLines      = 6,
                    shape         = KronoTokens.Shape.input,
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    supportingText = {
                        Text(
                            text     = "${message.length}/250",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                )

                Spacer(Modifier.height(KronoTokens.Spacing.sectionGap))

                // ── Feedbacks (Sucesso/Erro) ──────────────────
                AnimatedVisibility(visible = submitState is SubmitState.Success) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(bottom = KronoTokens.Spacing.md)
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(KronoTokens.Icon.status))
                        Spacer(Modifier.width(KronoTokens.Spacing.sm))
                        Text("Relatório enviado. Obrigado!", color = MaterialTheme.colorScheme.primary)
                    }
                }

                AnimatedVisibility(visible = submitState is SubmitState.Error) {
                    Text(
                        text     = "Falha ao enviar. Verifique sua conexão.",
                        color    = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = KronoTokens.Spacing.sm),
                        style    = MaterialTheme.typography.labelSmall
                    )
                }

                // ── Botão Enviar ──────────────────────────────
                Button(
                    onClick = {
                        scope.launch {
                            submitState = SubmitState.Loading
                            submitState = submitToGoogleForms(
                                name    = name.trim(),
                                email   = email.trim(),
                                message = message.trim(),
                                version = BuildConfig.VERSION_NAME
                            )
                        }
                    },
                    enabled  = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(KronoTokens.Button.height),
                    shape = KronoTokens.Shape.button
                ) {
                    if (submitState is SubmitState.Loading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(KronoTokens.Component.buttonSpinner),
                            strokeWidth = KronoTokens.Stroke.circularIndicator,
                            color       = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(KronoTokens.Button.iconSpacing))
                        Text("Enviando...")
                    } else {
                        Icon(Icons.Default.BugReport, null, modifier = Modifier.size(KronoTokens.Icon.button))
                        Spacer(Modifier.width(KronoTokens.Button.iconSpacing))
                        Text("Enviar Relatório", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private suspend fun submitToGoogleForms(
    name   : String,
    email  : String,
    message: String,
    version: String
): SubmitState = withContext(Dispatchers.IO) {
    try {
        val formUrl = "https://docs.google.com/forms/d/e/$FORM_ID/formResponse"
        val params  = buildString {
            append(URLEncoder.encode(ENTRY_NAME,    "UTF-8")).append("=").append(URLEncoder.encode(name,    "UTF-8")).append("&")
            append(URLEncoder.encode(ENTRY_EMAIL,   "UTF-8")).append("=").append(URLEncoder.encode(email,   "UTF-8")).append("&")
            append(URLEncoder.encode(ENTRY_MESSAGE, "UTF-8")).append("=").append(URLEncoder.encode(message, "UTF-8")).append("&")
            append(URLEncoder.encode(ENTRY_VERSION, "UTF-8")).append("=").append(URLEncoder.encode(version, "UTF-8"))
        }

        val connection = (URL(formUrl).openConnection() as HttpURLConnection).apply {
            requestMethod  = "POST"
            doOutput       = true
            connectTimeout = 10_000
            readTimeout    = 10_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }

        connection.outputStream.use { it.write(params.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        connection.disconnect()

        if (code in 200..399) SubmitState.Success else SubmitState.Error
    } catch (e: Exception) {
        SubmitState.Error
    }
}
