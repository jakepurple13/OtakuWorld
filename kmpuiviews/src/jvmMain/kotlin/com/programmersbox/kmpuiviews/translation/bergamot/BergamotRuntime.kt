package com.programmersbox.kmpuiviews.translation.bergamot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Runs Bergamot (Marian) translation on desktop. */
interface BergamotRuntime {
    /** Null when ready; otherwise a user-facing reason (e.g. "Install the translator (9 MB)"). */
    suspend fun unavailableReason(): String?
    suspend fun install(onProgress: (Float) -> Unit)   // no-op if nothing to install
    suspend fun translate(text: String, model: BergamotModelFiles): String
}

/** One Bergamot model: the int8 model, the optional shortlist, and the vocabs (same file twice for a shared vocab). */
data class BergamotModelFiles(val model: File, val lex: File?, val srcVocab: File, val trgVocab: File)

/** A pinned `translator-cli` build published as a release asset by `.github/workflows/bergamot-cli.yml`. */
data class BergamotCliAsset(val url: String, val sha256: String, val sizeLabel: String)

/**
 * Pinned CLI builds keyed by [bergamotPlatformKey]. Empty until the first `bergamot-cli` workflow release is
 * published: fill in the asset URL, sha256 and size per platform from that release.
 */
val pinnedBergamotCliAssets: Map<String, BergamotCliAsset> = emptyMap()

/** `macos-arm64`, `macos-x64`, `windows-x64`, `linux-x64`, or null for other platforms. */
fun bergamotPlatformKey(
    osName: String = System.getProperty("os.name"),
    arch: String = System.getProperty("os.arch"),
): String? {
    val os = when {
        osName.startsWith("Mac") -> "macos"
        osName.startsWith("Windows") -> "windows"
        osName.startsWith("Linux") -> "linux"
        else -> return null
    }
    val cpu = when (arch) {
        "aarch64", "arm64" -> "arm64"
        "amd64", "x86_64" -> "x64"
        else -> return null
    }
    return "$os-$cpu".takeIf { it != "windows-arm64" && it != "linux-arm64" }
}

/** The Marian options Firefox uses for its Bergamot models, plus this model's files. */
internal fun bergamotConfigYaml(model: BergamotModelFiles): String = buildString {
    fun quote(file: File) = "\"" + file.absolutePath.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    appendLine("models: [${quote(model.model)}]")
    appendLine("vocabs: [${quote(model.srcVocab)}, ${quote(model.trgVocab)}]")
    model.lex?.let { appendLine("shortlist: [${quote(it)}, false]") }
    appendLine("beam-size: 1")
    appendLine("normalize: 1.0")
    appendLine("word-penalty: 0")
    appendLine("max-length-break: 128")
    appendLine("mini-batch-words: 1024")
    appendLine("workspace: 128")
    appendLine("max-length-factor: 2.0")
    appendLine("skip-cost: true")
    appendLine("quiet: true")
    appendLine("quiet-translation: true")
    appendLine("gemm-precision: int8shiftAlphaAll")
    appendLine("alignment: soft")
}

/**
 * Bergamot through the native `translator-cli` from bergamot-translator (mozilla/translations `inference/`):
 * one process per call, text on stdin, translation on stdout.
 *
 * The CLI is, in order: [cliOverride] (env `BERGAMOT_CLI` by default), the copy installed under [installRoot],
 * or a download of [asset] checked against its pinned sha256.
 */
class BergamotCliRuntime(
    private val installRoot: File,
    private val cliOverride: File? = System.getenv("BERGAMOT_CLI")?.takeIf { it.isNotBlank() }?.let(::File),
    private val asset: BergamotCliAsset? = bergamotPlatformKey()?.let(pinnedBergamotCliAssets::get),
    private val threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
    private val timeoutSeconds: Long = 60,
) : BergamotRuntime {

    private val exeName = if (System.getProperty("os.name").startsWith("Windows")) "translator-cli.exe" else "translator-cli"
    private val installed get() = File(installRoot, exeName)
    private val configDir get() = File(installRoot, "configs")

    private fun cli(): File? = cliOverride?.takeIf { it.canExecute() } ?: installed.takeIf { it.canExecute() }

    override suspend fun unavailableReason(): String? = withContext(Dispatchers.IO) {
        when {
            cli() != null -> null
            asset != null -> "Install the translator (${asset.sizeLabel})"
            else -> "The offline translator isn't available for this computer yet"
        }
    }

    override suspend fun install(onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        if (cli() != null) return@withContext
        val asset = asset ?: throw IOException("No translator build for this computer")
        installRoot.mkdirs()
        val part = File(installRoot, "$exeName.part")
        try {
            val sha256 = download(asset.url, part, onProgress)
            if (sha256 != asset.sha256) throw IOException("checksum mismatch")
            part.setExecutable(true)
            if (!installed.delete() && installed.exists() || !part.renameTo(installed)) {
                throw IOException("Couldn't move the translator into place")
            }
            onProgress(1f)
        } finally {
            part.delete()
        }
    }

    override suspend fun translate(text: String, model: BergamotModelFiles): String {
        if (text.isBlank()) return text
        val cli = withContext(Dispatchers.IO) { cli() } ?: throw IOException("The translator isn't installed")
        val config = withContext(Dispatchers.IO) { writeConfig(model) }
        return withContext(Dispatchers.IO) {
            val process = ProcessBuilder(
                cli.absolutePath,
                "--model-config-paths", config.absolutePath,
                "--cpu-threads", threads.toString(),
            ).start()
            try {
                coroutineScope {
                    // Drain both pipes concurrently so a chatty stderr can't block the process.
                    val out = async { process.inputStream.readBytes().decodeToString() }
                    val err = async { process.errorStream.readBytes().decodeToString() }
                    process.outputStream.use { it.write(text.toByteArray()) }
                    if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                        // Kill first: the pipe readers above only finish once the process is gone.
                        process.destroyForcibly()
                        throw IOException("Translation timed out")
                    }
                    val stdout = out.await()
                    val stderr = err.await()
                    if (process.exitValue() != 0) {
                        throw IOException("Translator exited with ${process.exitValue()}: ${stderr.trim().takeLast(300)}")
                    }
                    stdout.trim()
                }
            } finally {
                process.destroyForcibly()
            }
        }
    }

    /** One config per model, rewritten each call so moved or replaced files are picked up. */
    private fun writeConfig(model: BergamotModelFiles): File {
        configDir.mkdirs()
        val yaml = bergamotConfigYaml(model)
        val name = MessageDigest.getInstance("SHA-256").digest(model.model.absolutePath.toByteArray())
            .take(8).joinToString("") { "%02x".format(it) }
        return File(configDir, "$name.yml").apply { writeText(yaml) }
    }

    /** Streams [url] into [part]; returns its sha256 as lowercase hex. */
    private suspend fun download(url: String, part: File, onProgress: (Float) -> Unit): String {
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        val response = client.send(HttpRequest.newBuilder(URI(url)).build(), HttpResponse.BodyHandlers.ofInputStream())
        if (response.statusCode() !in 200..299) throw IOException("HTTP ${response.statusCode()}")
        val total = response.headers().firstValueAsLong("Content-Length").orElse(-1)
        val digest = MessageDigest.getInstance("SHA-256")
        var written = 0L
        response.body().use { input ->
            part.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    written += read
                    if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 0.99f))
                }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
