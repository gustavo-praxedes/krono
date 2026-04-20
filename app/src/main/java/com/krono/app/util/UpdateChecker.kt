package com.krono.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ============================================================
// UpdateChecker.kt - Versão Otimizada
// ============================================================

private const val GITHUB_API_URL =
    "https://api.github.com/repos/gustavo-praxedes/krono/releases/latest"

private const val TIMEOUT_MS = 10_000

data class UpdateInfo(
    val tagName    : String,
    val releaseUrl : String,
    val downloadUrl: String?,
    val changelog  : String
)

sealed class UpdateResult {
    data class UpdateAvailable(val info: UpdateInfo) : UpdateResult()
    object UpToDate    : UpdateResult()
    object NetworkError: UpdateResult()
}

sealed class ChangelogResult {
    data class Success(val info: UpdateInfo) : ChangelogResult()
    object NetworkError: ChangelogResult()
}

/**
 * Verifica se há uma nova versão disponível no GitHub.
 * Adicionado parâmetro de tempo para evitar cache de rede do Android/GitHub.
 */
suspend fun checkForUpdate(currentVersion: String): UpdateResult =
    withContext(Dispatchers.IO) {
        try {
            // Adiciona um timestamp para forçar a busca de um JSON novo
            val freshUrl = "$GITHUB_API_URL?t=${System.currentTimeMillis()}"
            val url        = URL(freshUrl)

            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout    = TIMEOUT_MS
                requestMethod  = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                // Força a conexão a não usar cache local
                useCaches = false
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateResult.NetworkError
            }

            val body = connection.inputStream.bufferedReader().readText()
            connection.disconnect()

            val json       = JSONObject(body)
            val tagName    = json.getString("tag_name").removePrefix("v").trim()
            val releaseUrl = json.getString("html_url")
            val changelog  = json.optString("body", "Nenhuma nota de lançamento disponível.")

            var downloadUrl: String? = null
            try {
                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }
            } catch (_: Exception) {}

            // Comparação robusta de versões
            if (isNewerVersion(tagName, currentVersion)) {
                UpdateResult.UpdateAvailable(
                    UpdateInfo(tagName, releaseUrl, downloadUrl, changelog)
                )
            } else {
                UpdateResult.UpToDate
            }

        } catch (e: Exception) {
            UpdateResult.NetworkError
        }
    }

suspend fun getChangelog(): ChangelogResult = withContext(Dispatchers.IO) {
    try {
        val url = URL("$GITHUB_API_URL?t=${System.currentTimeMillis()}")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github.v3+json")
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            return@withContext ChangelogResult.NetworkError
        }

        val body = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val json = JSONObject(body)
        val tagName = json.getString("tag_name").removePrefix("v").trim()

        var downloadUrl: String? = null
        val assets = json.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }
        }

        ChangelogResult.Success(
            UpdateInfo(
                tagName = tagName,
                releaseUrl = json.getString("html_url"),
                downloadUrl = downloadUrl,
                changelog = json.optString("body", "")
            )
        )
    } catch (_: Exception) {
        ChangelogResult.NetworkError
    }
}

/**
 * Compara versões de forma segura, ignorando sufixos como -debug ou -dirty.
 * Ex: "2.4.1" vs "2.3.2-debug" -> retorna true
 */
private fun isNewerVersion(remote: String, local: String): Boolean {
    return try {
        // Extrai apenas os números de cada parte da versão
        val r = remote.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }

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