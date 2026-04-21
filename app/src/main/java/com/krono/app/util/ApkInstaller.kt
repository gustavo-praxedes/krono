package com.krono.app.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

sealed class DownloadStatus {
    data object NotDownloaded : DownloadStatus()
    data class Downloading(val percent: Int) : DownloadStatus()
    data object Completed : DownloadStatus()
    data class Failed(val error: String) : DownloadStatus()
}

object ApkInstaller {

    private var currentDownloadId: Long = -1

    fun startDownload(context: Context, downloadUrl: String, version: String): Long {
        val fileName = "krono_v${version.replace(".", "_")}.apk"

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
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
            downloadManager.query(query).use { cursor ->
                if (!cursor.moveToFirst()) {
                    if (getLatestApk(context)?.exists() == true) DownloadStatus.Completed
                    else DownloadStatus.NotDownloaded
                } else {
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val percent = if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
                            DownloadStatus.Downloading(percent)
                        }
                        DownloadManager.STATUS_PENDING -> DownloadStatus.Downloading(0)
                        DownloadManager.STATUS_PAUSED -> {
                            val percent = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                            DownloadStatus.Downloading(percent)
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.Completed
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                            DownloadStatus.Failed("Erro: $reason")
                        }
                        else -> DownloadStatus.Downloading(0)
                    }
                }
            }
        } catch (_: Exception) {
            if (getLatestApk(context)?.exists() == true) DownloadStatus.Completed
            else DownloadStatus.NotDownloaded
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
        val file = getDownloadedFile(context, version) ?: return
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) { }
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
        } catch (_: Exception) { }
    }
}