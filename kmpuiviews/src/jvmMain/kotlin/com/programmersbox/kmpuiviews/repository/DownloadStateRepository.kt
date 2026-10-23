package com.programmersbox.kmpuiviews.repository

import com.programmersbox.kmpextensionloader.ApkExtensionStore
import com.programmersbox.kmpextensionloader.ApkInstallException
import com.programmersbox.kmpextensionloader.ApkInstallFailure
import com.programmersbox.kmpuiviews.utils.DownloadAndInstallStatus
import com.programmersbox.kmpuiviews.utils.InstallErrorReason
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DownloadStateRepository(
    private val client: HttpClient,
    private val stores: List<ApkExtensionStore>,
    private val tempDir: File,
    private val reload: suspend () -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : DownloadStateInterface {
    private val state = MutableStateFlow<List<DownloadAndInstallState>>(emptyList())
    private val jobs = ConcurrentHashMap<String, Job>()

    override val downloadList: Flow<List<DownloadAndInstallState>> get() = state

    override fun cancelDownload(id: String) {
        jobs.remove(id)?.cancel()
        setStatus(id, DownloadAndInstallStatus.Cancelled)
    }

    override fun install(url: String): Flow<DownloadAndInstallStatus> = emptyFlow()

    override fun downloadAndInstall(url: String) = start(url)

    override fun downloadThenInstall(url: String) = start(url)

    private fun start(url: String) {
        val id = UUID.randomUUID().toString()
        val name = url.substringBefore('?').trimEnd('/').substringAfterLast('/')
        state.update { it + DownloadAndInstallState(url, name, id, DownloadAndInstallStatus.Downloading(0f)) }
        jobs[id] = scope.launch {
            val file = File(tempDir, "${UUID.randomUUID()}.apk")
            val result = try {
                tempDir.mkdirs()
                download(url, file, id)
                setStatus(id, DownloadAndInstallStatus.Installing)
                val store = stores.firstOrNull() ?: throw NoStoreException()
                store.install(file)
                reload()
                DownloadAndInstallStatus.Installed
            } catch (e: CancellationException) {
                setStatus(id, DownloadAndInstallStatus.Cancelled)
                throw e
            } catch (e: NoStoreException) {
                error(InstallErrorReason.UNKNOWN, e.message)
            } catch (e: ApkInstallException) {
                error(
                    when (e.failure) {
                        ApkInstallFailure.INVALID -> InstallErrorReason.INVALID
                        ApkInstallFailure.INCOMPATIBLE -> InstallErrorReason.INCOMPATIBLE
                        ApkInstallFailure.STORAGE -> InstallErrorReason.STORAGE
                    },
                    e.message,
                )
            } catch (e: Exception) {
                error(InstallErrorReason.GENERIC, e.message)
            } finally {
                file.delete()
                jobs.remove(id)
            }
            setStatus(id, result)
        }
    }

    private suspend fun download(url: String, file: File, id: String) {
        client.prepareGet(url).execute { response ->
            if (!response.status.isSuccess()) throw IOException("HTTP ${response.status.value}")
            val total = response.contentLength()?.takeIf { it > 0 }
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = 0L
            var lastPercent = 0
            file.outputStream().use { out ->
                while (true) {
                    val n = channel.readAvailable(buffer, 0, buffer.size)
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    read += n
                    if (total != null) {
                        val percent = (read * 100 / total).toInt().coerceIn(0, 100)
                        if (percent != lastPercent) {
                            lastPercent = percent
                            setStatus(id, DownloadAndInstallStatus.Downloading(percent / 100f))
                        }
                    }
                }
            }
        }
    }

    private fun error(reason: InstallErrorReason, message: String?) =
        DownloadAndInstallStatus.Error(reason, message ?: reason.name)

    private fun setStatus(id: String, status: DownloadAndInstallStatus) {
        state.update { list -> list.map { if (it.id == id) it.copy(status = status) else it } }
    }

    private class NoStoreException : Exception("No extension installer available")
}
