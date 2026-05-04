package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class DownloadStateRepository : DownloadStateInterface {
    override val downloadList: Flow<List<DownloadAndInstallState>>
        get() = emptyFlow()

    override fun cancelDownload(id: String) {

    }

    override fun install(url: String): Flow<DownloadAndInstallStatus> = emptyFlow()

    override fun downloadAndInstall(url: String) {

    }

    override fun downloadThenInstall(url: String) {

    }
}