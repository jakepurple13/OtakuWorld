package com.programmersbox.uiviews.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.transformWhile
import java.io.File
import kotlin.time.Duration.Companion.seconds

internal class DownloadAndInstaller(private val context: Context) {
    private val downloadManager by lazy { context.getSystemService<DownloadManager>()!! }
    private val downloadReceiver = DownloadCompletionReceiver()
    private val activeDownloads = hashMapOf<String, Long>()
    private val downloadsStateFlows = hashMapOf<Long, MutableStateFlow<InstallType>>()

    fun downloadAndInstall(
        url: String,
        destinationPath: String
    ): Flow<InstallType> {
        val oldDownload = activeDownloads[url]
        if (oldDownload != null) {
            deleteDownload(url)
        }

        downloadReceiver.register()

        val downloadUri = url.toUri()
        val request = DownloadManager.Request(downloadUri)
            .setTitle(destinationPath)
            .setMimeType(APK_MIME)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, downloadUri.lastPathSegment)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = downloadManager.enqueue(request)
        activeDownloads[url] = id

        val downloadStateFlow = MutableStateFlow(InstallType.Pending)
        downloadsStateFlows[id] = downloadStateFlow

        val pollStatusFlow = downloadStatusFlow(id).mapNotNull { downloadStatus ->
            when (downloadStatus) {
                DownloadManager.STATUS_PENDING -> InstallType.Pending
                DownloadManager.STATUS_RUNNING -> InstallType.Downloading
                else -> null
            }
        }

        return merge(downloadStateFlow, pollStatusFlow).transformWhile {
            emit(it)
            !it.isCompleted()
        }.onCompletion {
            deleteDownload(url)
        }
    }

    private fun downloadStatusFlow(id: Long): Flow<Int> = flow {
        val query = DownloadManager.Query().setFilterById(id)
        while (true) {
            val downloadStatus = downloadManager.query(query).use { cursor ->
                if (!cursor.moveToFirst()) return@flow
                cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            }

            emit(downloadStatus)

            if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL || downloadStatus == DownloadManager.STATUS_FAILED) {
                return@flow
            }

            delay(1.seconds)
        }
    }
        .distinctUntilChanged()

    fun installApk(downloadId: Long, uri: Uri) {
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            setDataAndType(uri, APK_MIME)
            putExtra(EXTRA_DOWNLOAD_ID, downloadId)
        }
        context.startActivity(installIntent)
    }

    fun updateInstallStep(downloadId: Long, step: InstallType) {
        downloadsStateFlows[downloadId]?.let { it.value = step }
    }

    private fun deleteDownload(pkgName: String) {
        val downloadId = activeDownloads.remove(pkgName)
        if (downloadId != null) {
            downloadManager.remove(downloadId)
            downloadsStateFlows.remove(downloadId)
        }
        if (activeDownloads.isEmpty()) {
            downloadReceiver.unregister()
        }
    }

    private inner class DownloadCompletionReceiver : BroadcastReceiver() {
        private var isRegistered = false

        fun register() {
            if (isRegistered) return
            isRegistered = true

            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            runCatching { context.registerReceiver(this, filter) }
                .onFailure {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(this, filter, Context.RECEIVER_EXPORTED)
                    }
                }
        }

        fun unregister() {
            if (!isRegistered) return
            isRegistered = false

            context.unregisterReceiver(this)
        }


        override fun onReceive(context: Context, intent: Intent?) {
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0) ?: return
            if (id !in activeDownloads.values) return
            val uri = downloadManager.getUriForDownloadedFile(id)
            if (uri == null) {
                updateInstallStep(id, InstallType.Error)
                return
            }

            val query = DownloadManager.Query().setFilterById(id)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val localUri = cursor.getString(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI),
                    ).removePrefix(FILE_SCHEME)

                    installApk(id, File(localUri).getUriCompat(context))
                }
            }
        }
    }

    companion object {
        const val APK_MIME = "application/vnd.android.package-archive"
        const val EXTRA_DOWNLOAD_ID = "ExtensionInstaller.extra.DOWNLOAD_ID"
        const val FILE_SCHEME = "file://"
    }
}

enum class InstallType {
    Idle, Pending, Downloading, Installing, Installed, Error;

    fun isCompleted(): Boolean {
        return this == Installed || this == Error || this == Idle
    }
}

fun File.getUriCompat(context: Context): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, "${context.packageName}.provider", this)
    } else {
        this.toUri()
    }
}