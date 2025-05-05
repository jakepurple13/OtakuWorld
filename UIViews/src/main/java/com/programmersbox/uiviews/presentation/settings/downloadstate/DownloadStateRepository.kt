package com.programmersbox.uiviews.presentation.settings.downloadstate

import android.content.Context
import androidx.core.net.toUri
import androidx.work.WorkManager
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import com.programmersbox.uiviews.checkers.DownloadAndInstallWorker
import com.programmersbox.uiviews.checkers.DownloadWorker
import java.io.File
import java.util.UUID

class DownloadStateRepository(
    private val context: Context,
    private val downloadAndInstaller: DownloadAndInstaller,
) {
    private val workManager = WorkManager.getInstance(context)
    val downloadList = DownloadAndInstallWorker.listToDownloads(context)

    fun cancelDownload(id: UUID) {
        workManager.cancelWorkById(id)
    }

    fun install(url: String) =
        downloadAndInstaller.install(File(context.cacheDir, "${url.toUri().lastPathSegment}.apk"))

    fun downloadAndInstall(url: String) {
        DownloadAndInstallWorker.downloadAndInstall(context, url)
    }

    fun downloadThenInstall(url: String) {
        DownloadWorker.downloadThenInstall(context, url)
    }
}