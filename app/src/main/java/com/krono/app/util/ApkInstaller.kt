package com.krono.app.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

sealed class DownloadStatus {
    data object NotDownloaded : DownloadStatus()
    data class Downloading(val percent: Int) : DownloadStatus()
    data object Completed : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
}

object ApkInstaller {

    private const val TAG = "ApkInstaller"
    private var currentDownloadId: Long = -1

    fun startDownload(context: Context, downloadUrl: String, version: String): Long {
        val fileName = "krono_v${version.replace(".", "_")}.apk"
        
        // Remove arquivo antigo se existir para evitar conflitos
        val oldFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (oldFile.exists()) oldFile.delete()

        val request = DownloadManager.Request(downloadUrl.toUri()).apply {
            setTitle("Baixando Krono v$version")
            setDescription("Atualização do app Krono")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        currentDownloadId = downloadManager.enqueue(request)

        return currentDownloadId
    }

    fun getDownloadStatus(context: Context): DownloadStatus {
        if (currentDownloadId == -1L) return DownloadStatus.NotDownloaded

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(currentDownloadId)

        return try {
            downloadManager.query(query)?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    if (getLatestApk(context)?.exists() == true) DownloadStatus.Completed
                    else DownloadStatus.NotDownloaded
                } else {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> {
                            val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val percent = if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
                            DownloadStatus.Downloading(percent)
                        }
                        DownloadManager.STATUS_PENDING -> DownloadStatus.Downloading(0)
                        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Completed
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            DownloadStatus.Failed("Erro: $reason")
                        }
                        else -> DownloadStatus.Downloading(0)
                    }
                }
            } ?: if (getLatestApk(context)?.exists() == true) DownloadStatus.Completed else DownloadStatus.NotDownloaded
        } catch (e: Exception) {
            DownloadStatus.NotDownloaded
        }
    }

    fun getLatestApk(context: Context): File? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return dir?.listFiles()
            ?.filter { it.name.startsWith("krono_v") && it.name.endsWith(".apk") }
            ?.maxByOrNull { it.lastModified() }
    }

    fun getDownloadedFile(context: Context, version: String): File? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "krono_v${version.replace(".", "_")}.apk"
        val file = File(dir, fileName)
        return if (file.exists()) file else getLatestApk(context)
    }

    fun installApk(context: Context, version: String) {
        val file = getDownloadedFile(context, version)
        
        if (file == null || !file.exists()) {
            Log.e(TAG, "Arquivo APK não encontrado")
            return
        }

        if (file.length() < 1024 * 100) { // Menos de 100KB é provável que esteja corrompido
            Log.e(TAG, "Arquivo APK muito pequeno (${file.length()} bytes), possivelmente corrompido")
            file.delete()
            return
        }

        // Verifica permissão (Android 8.0+)
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context, 
                "${context.packageName}.provider", 
                file
            )
            
            Log.d(TAG, "Iniciando instalação. File: ${file.absolutePath}, URI: $uri")

            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir instalador", e)
        }
    }

    fun resetDownload() {
        currentDownloadId = -1
    }

    fun cleanUpOldApks(context: Context) {
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadDir?.listFiles()?.forEach {
                if (it.name.startsWith("krono_v") && it.name.endsWith(".apk")) it.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
