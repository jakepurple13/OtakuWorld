package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import kotlinx.coroutines.flow.Flow

interface DownloadStateInterface {
    val downloadList: Flow<List<DownloadAndInstallState>>

    fun cancelDownload(id: String)

    fun install(url: String): Flow<DownloadAndInstallStatus>

    fun downloadAndInstall(url: String)

    fun downloadThenInstall(url: String)
}

data class DownloadAndInstallState(
    val url: String,
    val name: String,
    val id: String,
    val status: DownloadAndInstallStatus,
)