package com.programmersbox.uiviews.di.kmpinterop

import android.content.Context
import androidx.core.net.toUri
import androidx.work.WorkManager
import com.programmersbox.kmpuiviews.repository.DownloadStateInterface
import com.programmersbox.kmpuiviews.utils.DownloadAndInstaller
import com.programmersbox.uiviews.checkers.DownloadAndInstallWorker
import com.programmersbox.uiviews.checkers.DownloadWorker
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import java.util.UUID

class DownloadStateRepository(
    private val context: Context,
    private val downloadAndInstaller: DownloadAndInstaller,
) : DownloadStateInterface {
    private val workManager = WorkManager.Companion.getInstance(context)
    override val downloadList = DownloadAndInstallWorker.Companion.listToDownloads(context)

    override fun cancelDownload(id: String) {
        workManager.cancelWorkById(UUID.fromString(id))
    }

    override fun install(url: String) = downloadAndInstaller.install(
        PlatformFile(File(context.cacheDir, "${url.toUri().lastPathSegment}.apk"))
    )

    override fun downloadAndInstall(url: String) {
        DownloadAndInstallWorker.Companion.downloadAndInstall(context, url)
    }

    override fun downloadThenInstall(url: String) {
        DownloadWorker.Companion.downloadThenInstall(context, url)
    }
}