package com.programmersbox.kmpuiviews.utils

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import com.google.firebase.perf.trace
import com.programmersbox.kmpuiviews.logFirebaseMessage
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
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

//TODO: Also need a wrapper for it.
// Probably an expect/actual class
actual class DownloadAndInstaller(
    private val context: Context,
) {
    private val packageInstaller by lazy { PackageInstaller.getInstance(context) }
    private val packageUninstaller by lazy { PackageUninstaller.getInstance(context) }
    private val client = HttpClient()

    actual suspend fun uninstall(packageName: String) {
        packageUninstaller.createSession(packageName) {
            confirmation = Confirmation.IMMEDIATE
        }
            .await()
            .let {
                println(it)
                Toast.makeText(context, "Uninstalled $packageName", Toast.LENGTH_SHORT).show()
            }
    }

    actual fun downloadAndInstall(
        url: String,
        destinationPath: String,
        confirmationType: ConfirmationType,
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

                install(
                    PlatformFile(file),
                    confirmationType
                )
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

    actual fun download(
        url: String,
        destinationPath: String,
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

    actual fun install(
        file: PlatformFile,
        confirmationType: ConfirmationType,
    ): Flow<DownloadAndInstallStatus> = channelFlow {
        val sess = packageInstaller.createSession(
            when (val f = file.androidFile) {
                is AndroidFile.FileWrapper -> f.file.toUri()
                is AndroidFile.UriWrapper -> f.uri
            }
        ) {
            packageSource = PackageSource.DownloadedFile

            confirmation = when (confirmationType) {
                ConfirmationType.IMMEDIATE -> Confirmation.IMMEDIATE
                ConfirmationType.DEFERRED -> Confirmation.DEFERRED
            }
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
