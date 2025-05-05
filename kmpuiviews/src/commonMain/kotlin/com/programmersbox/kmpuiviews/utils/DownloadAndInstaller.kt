package com.programmersbox.kmpuiviews.utils

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

expect class DownloadAndInstaller {
    suspend fun uninstall(packageName: String)

    fun downloadAndInstall(
        url: String,
        destinationPath: String = "",
        confirmationType: ConfirmationType = ConfirmationType.IMMEDIATE,
    ): Flow<DownloadAndInstallStatus>

    fun download(
        url: String,
        destinationPath: String = "",
    ): Flow<DownloadAndInstallStatus>

    fun install(
        file: PlatformFile,
        confirmationType: ConfirmationType = ConfirmationType.IMMEDIATE,
    ): Flow<DownloadAndInstallStatus>
}

@Serializable
sealed class DownloadAndInstallStatus {
    @Serializable
    data class Downloading(val progress: Float) : DownloadAndInstallStatus()

    @Serializable
    data object Downloaded : DownloadAndInstallStatus()

    @Serializable
    data object Installing : DownloadAndInstallStatus()

    @Serializable
    data object Installed : DownloadAndInstallStatus()

    @Serializable
    data class Error(val message: String) : DownloadAndInstallStatus()
}

enum class ConfirmationType {
    IMMEDIATE,
    DEFERRED
}