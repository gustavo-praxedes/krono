package com.krono.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ============================================================
// UpdateChecker.kt
// Responsável por consultar a API do GitHub e retornar
// informações sobre a última release disponível.
//
// Usa JSONObject nativo (org.json) — sem dependências extras.
// Toda a lógica de rede roda em Dispatchers.IO.
// ============================================================

private const val GITHUB_API_URL =
    "https://api.github.com/repos/gustavo-praxedes/krono/releases/latest"

private const val TIMEOUT_MS = 10_000 // 10 segundos

data class UpdateInfo(
    val tagName    : String,  // ex: "1.2.0"
    val releaseUrl : String,  // página da release no GitHub
    val downloadUrl: String?  // link direto do APK (pode ser null)
)

// Resultado selado — evita uso de exceções como controle de fluxo
sealed class UpdateResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateResult()
    object UpToDate    : UpdateResult()
    object NetworkError: UpdateResult()
}

// Verifica se há uma nova versão disponível no GitHub.
// Deve ser chamada dentro de um escopo com Dispatchers.IO.
suspend fun checkForUpdate(currentVersion: String): UpdateResult =
    withContext(Dispatchers.IO) {
        try {
            val url        = URL(GITHUB_API_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout    = TIMEOUT_MS
                requestMethod  = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
            }

            val responseCode = connection.responseCode

            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateResult.NetworkError
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val json       = JSONObject(body)
            val tagName    = json.getString("tag_name").removePrefix("v")
            val releaseUrl = json.getString("html_url")

            // Tenta obter o link direto do APK no primeiro asset
            val downloadUrl: String? = try {
                val assets = json.getJSONArray("assets")
                if (assets.length() > 0) {
                    assets.getJSONObject(0).getString("browser_download_url")
                } else null
            } catch (_: Exception) { null }

            // Compara versões semanticamente (major.minor.patch)
            if (isNewerVersion(tagName, currentVersion)) {
                UpdateResult.UpdateAvailable(
                    UpdateInfo(
                        tagName     = tagName,
                        releaseUrl  = releaseUrl,
                        downloadUrl = downloadUrl
                    )
                )
            } else {
                UpdateResult.UpToDate
            }

        } catch (_: Exception) {
            // Falha de conexão, timeout, JSON inválido — silencioso
            UpdateResult.NetworkError
        }
    }

// Compara duas versões no formato "X.Y.Z".
// Retorna true se remoteVersion for maior que localVersion.
private fun isNewerVersion(remote: String, local: String): Boolean {
    return try {
        val r = remote.split(".").map { it.toInt() }
        val l = local.split(".").map  { it.toInt() }
        val size = maxOf(r.size, l.size)
        for (i in 0 until size) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        false
    } catch (_: Exception) {
        false
    }
}