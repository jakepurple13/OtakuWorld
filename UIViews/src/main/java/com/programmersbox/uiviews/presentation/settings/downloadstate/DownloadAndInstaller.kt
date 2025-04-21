package com.programmersbox.uiviews.presentation.settings.downloadstate

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import com.google.firebase.perf.trace
import com.programmersbox.uiviews.utils.logFirebaseMessage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.Serializable
import ru.solrudev.ackpine.installer.InstallFailure
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.parameters.InstallConstraints
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.state
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.createSession
import java.io.File
import kotlin.time.Duration.Companion.minutes

class DownloadAndInstaller(
    private val context: Context,
) {
    private val packageInstaller by lazy { PackageInstaller.getInstance(context) }
    private val packageUninstaller by lazy { PackageUninstaller.getInstance(context) }
    private val client = HttpClient()

    suspend fun uninstall(packageName: String) {
        packageUninstaller.createSession(packageName) {
            confirmation = Confirmation.IMMEDIATE
        }
            .await()
            .let {
                println(it)
                Toast.makeText(context, "Uninstalled $packageName", Toast.LENGTH_SHORT).show()
            }
    }

    fun downloadAndInstall(
        url: String,
        destinationPath: String = "",
        confirmationType: Confirmation = Confirmation.IMMEDIATE,
    ): Flow<DownloadAndInstallStatus> {
        val file = File(context.cacheDir, "${url.toUri().lastPathSegment}.apk")

        return channelFlow<DownloadAndInstallStatus> {
            trace("download_and_install") {
                client.prepareGet(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        send(DownloadAndInstallStatus.Downloading(bytesSentTotal.toFloat() / (contentLength ?: 1L)))
                    }
                }.execute {
                    it.bodyAsChannel().copyAndClose(file.writeChannel())
                    send(DownloadAndInstallStatus.Downloaded)
                }

                println("Starting Install Session")

                install(file, confirmationType)
                    .onEach { send(it) }
                    .launchIn(this@channelFlow)
            }
        }
            .catch { it.printStackTrace() }
            .onEach {
                println(it)
                if (it !is DownloadAndInstallStatus.Downloading) logFirebaseMessage(it.toString())
                if (it is DownloadAndInstallStatus.Installed) file.delete()
            }
    }

    fun download(
        url: String,
        destinationPath: String = "",
    ): Flow<DownloadAndInstallStatus> {
        val file = File(context.cacheDir, "${url.toUri().lastPathSegment}.apk")

        return channelFlow<DownloadAndInstallStatus> {
            trace("download") {
                client.prepareGet(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        send(DownloadAndInstallStatus.Downloading(bytesSentTotal.toFloat() / (contentLength ?: 1L)))
                    }
                }.execute {
                    it.bodyAsChannel().copyAndClose(file.writeChannel())
                    send(DownloadAndInstallStatus.Downloaded)
                }
            }
        }
            .catch { it.printStackTrace() }
            .onEach {
                println(it)
                if (it !is DownloadAndInstallStatus.Downloading) logFirebaseMessage(it.toString())
                if (it is DownloadAndInstallStatus.Installed) file.delete()
            }
    }

    fun install(
        file: File,
        confirmationType: Confirmation = Confirmation.IMMEDIATE,
    ): Flow<DownloadAndInstallStatus> = channelFlow {
        val sess = packageInstaller.createSession(file.toUri()) {
            packageSource = PackageSource.DownloadedFile

            confirmation = confirmationType
            installerType = InstallerType.SESSION_BASED

            constraints = InstallConstraints.gentleUpdate(
                timeout = 1.minutes,
                timeoutStrategy = InstallConstraints.TimeoutStrategy.Fail
            )

            requireUserAction = true
            requestUpdateOwnership = true
        }

        sess
            .state
            .map { s ->
                when (s) {
                    is Session.State.Failed<*> -> DownloadAndInstallStatus.Error(
                        when (val failure = s.failure) {
                            is InstallFailure.Aborted -> "Aborted"
                            is InstallFailure.Blocked -> "Blocked by ${failure.otherPackageName}"
                            is InstallFailure.Conflict -> "Conflicting with ${failure.otherPackageName}"
                            is InstallFailure.Exceptional -> failure.exception.message
                            is InstallFailure.Generic -> "Generic failure"
                            is InstallFailure.Incompatible -> "Incompatible"
                            is InstallFailure.Invalid -> "Invalid"
                            is InstallFailure.Storage -> "Storage path: ${failure.storagePath}"
                            is InstallFailure.Timeout -> "Timeout"
                            else -> "Unknown failure"
                        }.orEmpty()
                    )

                    Session.State.Succeeded -> DownloadAndInstallStatus.Installed
                    else -> DownloadAndInstallStatus.Installing
                }
            }
            .onEach { send(it) }
            .launchIn(this@channelFlow)

        sess.await()
    }
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