package com.programmersbox.kmpuiviews.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.google.firebase.perf.trace
import com.programmersbox.kmpuiviews.logFirebaseMessage
import com.programmersbox.kmpuiviews.repository.InstallStatusRepository
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformWhile
import java.io.File

actual class DownloadAndInstaller(
    private val context: Context,
    private val packageInstallEngine: PackageInstallEngine,
    private val installStatusRepository: InstallStatusRepository,
) {
    private val client = HttpClient()

    actual suspend fun uninstall(packageName: String) {
        context.startActivity(
            Intent(Intent.ACTION_UNINSTALL_PACKAGE, "package:$packageName".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
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

                printLogs { "Starting Install Session" }

                install(PlatformFile(file), confirmationType)
                    .onEach { send(it) }
                    .launchIn(this@channelFlow)
            }
        }
            .catch {
                it.printStackTrace()
                emit(DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, it.message ?: "Unknown error"))
            }
            .onEach {
                printLogs { it }
                if (it !is DownloadAndInstallStatus.Downloading) logFirebaseMessage(it.toString())
            }
            .onCompletion { cause -> if (cause != null) file.delete() }
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
            .catch {
                it.printStackTrace()
                emit(DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, it.message ?: "Unknown error"))
            }
            .onEach {
                printLogs { it }
                if (it !is DownloadAndInstallStatus.Downloading) logFirebaseMessage(it.toString())
            }
            .onCompletion { cause -> if (cause != null) file.delete() }
    }

    actual fun install(
        file: PlatformFile,
        confirmationType: ConfirmationType,
    ): Flow<DownloadAndInstallStatus> {
        var sessionId: Int? = null
        var terminalReached = false

        return flow {
            if (!packageInstallEngine.canRequestPackageInstalls()) {
                emit(DownloadAndInstallStatus.PermissionRequired)
                return@flow
            }

            val localFile = resolveLocalFile(file)

            sessionId = runCatching { packageInstallEngine.commit(localFile) }
                .onFailure {
                    emit(DownloadAndInstallStatus.Error(InstallErrorReason.GENERIC, it.message ?: "Unable to start install"))
                }
                .getOrNull()
            val id = sessionId ?: return@flow

            installStatusRepository.registerTempFile(id, localFile)
            emit(DownloadAndInstallStatus.Installing)

            emitAll(
                installStatusRepository.flowFor(id).transformWhile { status ->
                    emit(status)
                    val terminal = status is DownloadAndInstallStatus.Installed ||
                        status is DownloadAndInstallStatus.Cancelled ||
                        status is DownloadAndInstallStatus.Error
                    if (terminal) terminalReached = true
                    !terminal
                }
            )
        }.onCompletion {
            val id = sessionId
            if (id != null && !terminalReached) {
                packageInstallEngine.abandon(id)
                installStatusRepository.consumeTempFile(id)?.delete()
                installStatusRepository.clear(id)
            }
        }
    }

    private fun resolveLocalFile(file: PlatformFile): File =
        when (val androidFile = file.androidFile) {
            is AndroidFile.FileWrapper -> androidFile.file
            is AndroidFile.UriWrapper -> File(context.cacheDir, "install_${androidFile.uri.hashCode()}.apk").also { copy ->
                context.contentResolver.openInputStream(androidFile.uri)?.use { input ->
                    copy.outputStream().use { input.copyTo(it) }
                }
            }
        }
}
