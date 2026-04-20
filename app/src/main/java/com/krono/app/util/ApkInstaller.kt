package com.krono.app.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    fun downloadAndInstall(
        context    : Context,
        downloadUrl: String,
        version    : String,
        onProgress : (Float) -> Unit,   // 0f..1f
        onError    : (String) -> Unit
    ) {
        val fileName = "krono-$version.apk"
        val destFile = File(context.cacheDir, fileName)

        // Limpa APK anterior se existir
        if (destFile.exists()) destFile.delete()

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Krono $version")
            .setDescription("Baixando atualização...")
            .setDestinationUri(Uri.fromFile(destFile))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = dm.enqueue(request)

        // Observa progresso em background thread
        Thread {
            var downloading = true
            while (downloading) {
                val query  = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesCol  = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalCol  = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status    = if (statusCol >= 0) cursor.getInt(statusCol) else -1
                    val bytes     = if (bytesCol  >= 0) cursor.getLong(bytesCol) else 0L
                    val total     = if (totalCol  >= 0) cursor.getLong(totalCol) else 1L

                    if (total > 0) onProgress(bytes.toFloat() / total.toFloat())

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            installApk(context, destFile, onError)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            val reasonCol = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason    = if (reasonCol >= 0) cursor.getInt(reasonCol) else -1
                            onError("Download falhou (código $reason)")
                            destFile.delete()
                        }
                    }
                    cursor.close()
                }
                if (downloading) Thread.sleep(500)
            }
        }.start()
    }

    private fun installApk(context: Context, file: File, onError: (String) -> Unit) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Registra receiver para deletar o APK após instalação
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, i: Intent) {
                    file.delete()
                    try { ctx.unregisterReceiver(this) } catch (_: Exception) { }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
                        addDataScheme("package")
                    },
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(
                    receiver,
                    IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
                        addDataScheme("package")
                    }
                )
            }

            context.startActivity(intent)

        } catch (e: Exception) {
            onError("Erro ao instalar: ${e.message}")
            file.delete()
        }
    }
}