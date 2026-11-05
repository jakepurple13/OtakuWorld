package com.programmersbox.anime.shared.subtitlegen

import com.programmersbox.anime.shared.AnimeDesktopSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/** A pinned whisper.cpp release zip. */
data class WhisperCliAsset(val version: String, val fileName: String, val sha256: String, val sizeLabel: String)

/** The official whisper.cpp Windows build for [arch] (`os.arch`), or null when there is none. */
fun windowsWhisperAsset(arch: String): WhisperCliAsset? = when (arch) {
    "amd64", "x86_64" -> WhisperCliAsset(
        "b5454", "whisper-bin-x64.zip", "6ba69e3482d7826214f90a6a9c84ca07782aec1e1d0c6a7c30c994fd5d816ccb", "9 MB",
    )
    "aarch64", "arm64" -> WhisperCliAsset(
        "b5454", "whisper-bin-win-cpu-arm64.zip", "28c37e7b598c3d9bbfef94f3bd67f2ee6f12b7c86f3da5edcf5e4308d615ff6d", "4 MB",
    )
    else -> null
}

sealed interface CliInstall {
    /** Not Windows, or no build for this CPU: whisper-cli must be installed by the user. */
    data object NotNeeded : CliInstall
    data object Missing : CliInstall
    data class Installing(val fraction: Float) : CliInstall
    data class Ready(val file: File) : CliInstall
    /** The last install failed; calling `install()` again retries. */
    data class Failed(val message: String) : CliInstall
}

private const val CLI_EXE = "whisper-cli.exe"

/** Thrown by [WhisperCliInstaller.ensureInstalled] when the install fails; [message] is the Failed state's message. */
class WhisperCliDownloadException(message: String) : IOException(message)

/**
 * Downloads whisper.cpp's Windows build into `<installRoot>/<version>/`: whisper-cli.exe plus the dlls it loads.
 * The zip goes to `<fileName>.part`, is checked against its sha256, and is extracted into `<version>.tmp/`, which
 * is renamed into place on success.
 */
class WhisperCliInstaller(
    private val installRoot: File,
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    isWindows: Boolean = System.getProperty("os.name").startsWith("Windows"),
    arch: String = System.getProperty("os.arch"),
    private val baseUrl: String = "https://github.com/ggml-org/whisper.cpp/releases/download/",
    /** The zip to install; tests pass their own so the checksum matches. */
    val asset: WhisperCliAsset? = if (isWindows) windowsWhisperAsset(arch) else null,
    /** Renames a directory; tests make it fail to exercise the retry. */
    private val rename: (from: File, to: File) -> Boolean = { from, to -> from.renameTo(to) },
) {
    private val lock = Any()
    private var job: Job? = null

    private val _state = MutableStateFlow(
        when {
            asset == null -> CliInstall.NotNeeded
            installedExe(asset).isFile -> CliInstall.Ready(installedExe(asset))
            else -> CliInstall.Missing
        }
    )
    val state: StateFlow<CliInstall> = _state.asStateFlow()

    fun install() {
        startInstall()
    }

    /** whisper-cli.exe, installing it first if needed; null when this platform has nothing to install. */
    suspend fun ensureInstalled(onProgress: (Float) -> Unit): File? {
        when (val s = state.value) {
            CliInstall.NotNeeded -> return null
            is CliInstall.Ready -> return s.file
            else -> Unit
        }
        val job = startInstall()
        if (job != null) coroutineScope {
            val progress = launch { state.collect { (it as? CliInstall.Installing)?.let { s -> onProgress(s.fraction) } } }
            job.join()
            progress.cancel()
        }
        return when (val s = state.value) {
            is CliInstall.Ready -> s.file.also { onProgress(1f) }
            is CliInstall.Failed -> throw WhisperCliDownloadException(s.message)
            else -> throw WhisperCliDownloadException("Download of whisper.cpp was cancelled")
        }
    }

    private fun installedExe(asset: WhisperCliAsset) = File(File(installRoot, asset.version), CLI_EXE)

    /**
     * Replaces [target] with [tmp]. Antivirus scanners (Windows Defender) briefly hold handles on freshly written
     * .exe/.dll files, which makes the delete or rename fail, so this retries with a growing delay before giving up.
     */
    private suspend fun moveIntoPlace(tmp: File, target: File) {
        repeat(5) { attempt ->
            if (attempt > 0) delay(200L * attempt)
            if (target.deleteRecursively() && rename(tmp, target)) return
        }
        throw IOException("Couldn't move ${tmp.name} into place")
    }

    /** The running install, a new one, or null when there is nothing to install. */
    private fun startInstall(): Job? = synchronized(lock) {
        val asset = asset ?: return null
        job?.takeIf { it.isActive }?.let { return it }
        if (_state.value is CliInstall.Ready) return null
        _state.value = CliInstall.Installing(0f)
        scope.launch(Dispatchers.IO) {
            val self = currentCoroutineContext()[Job]
            val part = File(installRoot, asset.fileName + ".part")
            val tmp = File(installRoot, asset.version + ".tmp")
            val target = File(installRoot, asset.version)
            var result: CliInstall = CliInstall.Missing
            try {
                installRoot.mkdirs()
                val sha256 = download(asset, part)
                if (sha256 != asset.sha256) throw IOException("checksum mismatch")
                if (!tmp.deleteRecursively()) throw IOException("Couldn't clear ${tmp.name}")
                tmp.mkdirs()
                extract(part, tmp)
                if (!File(tmp, CLI_EXE).isFile) throw IOException("$CLI_EXE missing from ${asset.fileName}")
                currentCoroutineContext().ensureActive()
                moveIntoPlace(tmp, target)
                result = CliInstall.Ready(File(target, CLI_EXE))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                result = CliInstall.Failed("Download failed: " + (e.message ?: e::class.simpleName))
            } finally {
                part.delete()
                tmp.deleteRecursively()
                // Cleared together with the final state, so install() right after a failure starts a retry.
                synchronized(lock) {
                    _state.value = result
                    if (job === self) job = null
                }
            }
        }.also { job = it }
    }

    /** Streams the zip into [part]; returns its sha256 as lowercase hex. */
    private suspend fun download(asset: WhisperCliAsset, part: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val request = Request.Builder().url(baseUrl + asset.version + "/" + asset.fileName).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body
            val total = body.contentLength()
            var written = 0L
            var lastPercent = -1
            var lastAt = 0L
            body.byteStream().use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        written += read
                        val fraction = if (total > 0) (written.toFloat() / total).coerceIn(0f, 1f) else 0f
                        // Throttle: publish when the percent changes or 100 ms passed.
                        val percent = (fraction * 100).toInt()
                        val now = System.nanoTime()
                        if (percent != lastPercent || now - lastAt >= 100_000_000L) {
                            lastPercent = percent
                            lastAt = now
                            _state.value = CliInstall.Installing(fraction)
                        }
                    }
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Copies `Release/whisper-cli.exe` and the `.dll` files directly under `Release/` flat into [dir]; skips the rest. */
    private suspend fun extract(zip: File, dir: File) {
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                currentCoroutineContext().ensureActive()
                val name = entry.name.removePrefix("Release/")
                // Only direct children of Release/: nothing may land outside [dir].
                val safe = entry.name.startsWith("Release/") && !entry.isDirectory && name.isNotEmpty() &&
                    '/' !in name && '\\' !in name && ".." !in name
                if (safe && (name == CLI_EXE || name.endsWith(".dll"))) {
                    File(dir, name).outputStream().use { input.copyTo(it) }
                }
            }
        }
    }
}

/**
 * The whisper-cli to run, without downloading: the configured path when runnable, else the copy AnimeWorld
 * installed, else [search] (PATH and the usual install dirs).
 */
suspend fun resolveWhisperCli(
    settings: AnimeDesktopSettings,
    installer: WhisperCliInstaller?,
    search: suspend () -> File? = { findWhisperCli("") },
): File? {
    val configured = settings.whisperPath.get().takeIf { it.isNotBlank() }?.let(::File)
    if (configured != null && withContext(Dispatchers.IO) { configured.canExecute() }) return configured
    (installer?.state?.value as? CliInstall.Ready)?.let { return it.file }
    return search()
}

/** [resolveWhisperCli], else whisper.cpp downloaded by [installer] (Windows only); null when there is none. */
suspend fun locateWhisperCli(
    settings: AnimeDesktopSettings,
    installer: WhisperCliInstaller?,
    onProgress: (Float) -> Unit,
    search: suspend () -> File? = { findWhisperCli("") },
): File? = resolveWhisperCli(settings, installer, search) ?: installer?.ensureInstalled(onProgress)
