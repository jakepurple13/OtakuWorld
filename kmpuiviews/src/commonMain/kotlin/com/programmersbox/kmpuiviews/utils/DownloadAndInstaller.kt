package com.programmersbox.kmpuiviews.utils

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

expect class DownloadAndInstaller {
    suspend fun uninstall(packageName: String)

    fun downloadAndInstall(
        url: String,
        destinationPath: String = "",
        confirmationType: ConfirmationType = ConfirmationType.DEFERRED,
    ): Flow<DownloadAndInstallStatus>

    fun download(
        url: String,
        destinationPath: String = "",
    ): Flow<DownloadAndInstallStatus>

    fun install(
        file: PlatformFile,
        confirmationType: ConfirmationType = ConfirmationType.DEFERRED,
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
    data object PendingUserAction : DownloadAndInstallStatus()

    @Serializable
    data object PermissionRequired : DownloadAndInstallStatus()

    @Serializable
    data object Installed : DownloadAndInstallStatus()

    @Serializable
    data object Cancelled : DownloadAndInstallStatus()

    @Serializable
    data class Error(val reason: InstallErrorReason, val message: String) : DownloadAndInstallStatus()
}

@Serializable
enum class InstallErrorReason {
    BLOCKED, CONFLICT, INCOMPATIBLE, INVALID, STORAGE, GENERIC, UNKNOWN,
}

enum class ConfirmationType {
    IMMEDIATE,
    DEFERRED
}