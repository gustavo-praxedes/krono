package com.krono.app.data

import java.util.Locale

// ============================================================
// TimeUtils.kt
// Funções utilitárias de formatação de tempo.
// Removida msToHours() — não era utilizada em nenhum lugar.
// ============================================================

fun Long.toFormattedTime(
    showHours: Boolean = true,
    showSeconds: Boolean = true
): String {
    val totalSeconds = this / 1000L
    val hours        = totalSeconds / 3600L
    val minutes      = (totalSeconds % 3600L) / 60L
    val seconds      = totalSeconds % 60L

    return when {
        showHours && showSeconds ->
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)

        !showHours && showSeconds -> {
            val totalMinutes = totalSeconds / 60L
            val secs         = totalSeconds % 60L
            String.format(Locale.ROOT, "%02d:%02d", totalMinutes, secs)
        }

        showHours && !showSeconds ->
            String.format(Locale.ROOT, "%02d:%02d", hours, minutes)

        else -> {
            val totalMinutes = totalSeconds / 60L
            String.format(Locale.ROOT, "%02d", totalMinutes)
        }
    }
}

// Converte string "HHHH:MM:SS" para segundos totais.
// Retorna null se o formato for inválido.
fun parseTimeLimitInput(input: String): Long? {
    val parts = input.split(":")
    if (parts.size != 3) return null
    val h = parts[0].toLongOrNull() ?: return null
    val m = parts[1].toLongOrNull() ?: return null
    val s = parts[2].toLongOrNull() ?: return null
    if (m > 59 || s > 59) return null
    return h * 3600L + m * 60L + s
}

// Converte segundos totais para string "HHHH:MM:SS"
fun formatTimeLimitSeconds(totalSeconds: Long): String {
    if (totalSeconds <= 0L) return "0000:00:00"
    val h = totalSeconds / 3600L
    val m = (totalSeconds % 3600L) / 60L
    val s = totalSeconds % 60L
// Use String.format com Locale.ROOT para evitar problemas de idioma
    return String.format(Locale.ROOT, "%04d:%02d:%02d", h, m, s)
}

// Formata milissegundos em "HHh MMm SSs" para exibição no diálogo de doação.
// Exemplos:
//   43_200_000L → "12h 00m 00s"
//   90_061_000L → "25h 01m 01s"
fun formatLifetimeDetailed(totalMs: Long): String {
    if (totalMs <= 0L) return "0h 00m 00s"
    val totalSeconds = totalMs / 1000L
    val h = totalSeconds / 3600L
    val m = (totalSeconds % 3600L) / 60L
    val s = totalSeconds % 60L
    return String.format(Locale.ROOT, "%dh %02dm %02ds", h, m, s)
}