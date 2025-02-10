package com.programmersbox.uiviews.utils

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import com.google.firebase.perf.trace
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.installer.parameters.PackageSource
import ru.solrudev.ackpine.session.Failure
import ru.solrudev.ackpine.session.Session
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import ru.solrudev.ackpine.session.state
import ru.solrudev.ackpine.uninstaller.PackageUninstaller
import ru.solrudev.ackpine.uninstaller.createSession
import java.io.File

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
        destinationPath: String,
    ): Flow<DownloadAndInstallStatus> {
        val file = File(context.cacheDir, "${url.toUri().lastPathSegment}.apk")

        return channelFlow<DownloadAndInstallStatus> {
            trace("download_and_install") {
                //TODO: Show notification for progress
                client.prepareGet(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        send(DownloadAndInstallStatus.Downloading(bytesSentTotal.toFloat() / (contentLength ?: 1L)))
                    }
                }.execute {
                    it.bodyAsChannel().copyAndClose(file.writeChannel())
                    send(DownloadAndInstallStatus.Downloaded)
                }

                println("Starting Install Session")

                val sess = packageInstaller.createSession(file.toUri()) {
                    packageSource = PackageSource.DownloadedFile

                    confirmation = Confirmation.DEFERRED
                    installerType = InstallerType.SESSION_BASED

                    requireUserAction = true
                    requestUpdateOwnership = true
                }

                sess
                    .state
                    .map { s ->
                        when (s) {
                            is Session.State.Failed<*> -> DownloadAndInstallStatus.Error(s.failure)
                            Session.State.Succeeded -> DownloadAndInstallStatus.Installed
                            else -> DownloadAndInstallStatus.Installing
                        }
                    }
                    .onEach { send(it) }
                    .launchIn(this@channelFlow)

                sess.await()
            }
        }
            .onEach {
                println(it)
                if (it !is DownloadAndInstallStatus.Downloading) logFirebaseMessage(it.toString())
                if (it is DownloadAndInstallStatus.Installed) file.delete()
            }
    }
}

sealed class DownloadAndInstallStatus {
    data class Downloading(val progress: Float) : DownloadAndInstallStatus()
    data object Downloaded : DownloadAndInstallStatus()
    data object Installing : DownloadAndInstallStatus()
    data object Installed : DownloadAndInstallStatus()
    data class Error(val failure: Failure) : DownloadAndInstallStatus()
}