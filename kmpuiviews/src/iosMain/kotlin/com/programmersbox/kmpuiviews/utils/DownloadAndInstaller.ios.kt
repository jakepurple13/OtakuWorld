package com.programmersbox.kmpuiviews.utils

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow

actual class DownloadAndInstaller {
    actual suspend fun uninstall(packageName: String) {
    }

    actual fun downloadAndInstall(
        url: String,
        destinationPath: String,
        confirmationType: ConfirmationType,
    ): Flow<DownloadAndInstallStatus> {
        TODO("Not yet implemented")
    }

    actual fun download(
        url: String,
        destinationPath: String,
    ): Flow<DownloadAndInstallStatus> {
        TODO("Not yet implemented")
    }

    actual fun install(
        file: PlatformFile,
        confirmationType: ConfirmationType,
    ): Flow<DownloadAndInstallStatus> {
        TODO("Not yet implemented")
    }
}