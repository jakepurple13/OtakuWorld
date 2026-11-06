package com.programmersbox.kmpuiviews.translation.bergamot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
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

    /**
     * False when this computer has no translator build, so [install] can't help: hide Install and show
     * [unavailableReason] (which then suggests another engine) instead.
     */
    val isSupported: Boolean get() = true
}

/** One Bergamot model: the int8 model, the optional shortlist, and the vocabs (same file twice for a shared vocab). */
data class BergamotModelFiles(val model: File, val lex: File?, val srcVocab: File, val trgVocab: File)

/** A pinned `translator-cli` build published as a release asset by `.github/workflows/bergamot-cli.yml`. */
data class BergamotCliAsset(val url: String, val sha256: String, val sizeLabel: String)

private const val BERGAMOT_CLI_RELEASE = "https://github.com/jakepurple13/OtakuWorld/releases/download/bergamot-cli-0.1.1/"

/**
 * Pinned CLI builds keyed by [bergamotPlatformKey], from the `bergamot-cli-0.1.1` release (its SHA256SUMS).
 * windows-x64 has no build yet.
 */
val pinnedBergamotCliAssets: Map<String, BergamotCliAsset> = mapOf(
    "macos-arm64" to BergamotCliAsset(
        BERGAMOT_CLI_RELEASE + "translator-cli-macos-arm64",
        "bd65f0f2e63259fe3cde393d66aeddbfcfb0e1a52787e7f7daa919a4cb879eaf",
        "7.7 MB",
    ),
    "macos-x64" to BergamotCliAsset(
        BERGAMOT_CLI_RELEASE + "translator-cli-macos-x64",
        "cb8c52f416f677c6d9ba9b5792428b77d71efcff82539e09730b760c8f5b8e7e",
        "8.6 MB",
    ),
    "linux-x64" to BergamotCliAsset(
        BERGAMOT_CLI_RELEASE + "translator-cli-linux-x64",
        "a545695ccd9c7b6571713675384d43b8d6ef814fe5d18850b2eb457dae4e2eb1",
        "38 MB",
    ),
)

/** Shown when there is no pinned build for [platformKey] (a [bergamotPlatformKey] value, or null). */
fun bergamotUnsupportedReason(platformKey: String?): String {
    val os = when (platformKey?.substringBefore('-')) {
        "macos" -> "this Mac"
        "windows" -> "Windows"
        "linux" -> "this Linux computer"
        else -> "this computer"
    }
    return "Bergamot isn't available for $os yet. Use Ollama or Custom instead."
}

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

/**
 * Bergamot joins translated sentences with the whitespace that followed each source sentence. Japanese and Chinese
 * put none after `。`, so English output comes back glued: "quietly.Then she laughed." This adds a space where
 * `.`/`!`/`?` (plus an optional closing curly quote or bracket) after a lowercase letter or digit runs straight into
 * a capitalized word, an opening curly quote, or `(`.
 *
 * Left alone: decimals ("3.14"), abbreviations ("e.g. this", "U.S.A."), all-caps words after a dot ("Node.JS",
 * "example.COM"), and anything next to a straight `"`, which can't be told apart as opening or closing
 * (`quietly."Then` stays as is).
 */
fun spaceBetweenSentences(text: String): String = sentenceGlue.replace(text, "$1 ")

private val sentenceGlue = Regex("""(?<=[\p{Ll}\p{Nd}])([.!?]+[”’)\]]?)(?=\p{Lu}(?!\p{Lu})|[“‘(])""")

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
    private val platformKey: String? = bergamotPlatformKey(),
    private val asset: BergamotCliAsset? = platformKey?.let(pinnedBergamotCliAssets::get),
    private val threads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4),
    private val timeoutSeconds: Long = 60,
) : BergamotRuntime {

    private val exeName = if (System.getProperty("os.name").startsWith("Windows")) "translator-cli.exe" else "translator-cli"
    private val installed get() = File(installRoot, exeName)
    private val configDir get() = File(installRoot, "configs")

    private fun cli(): File? = cliOverride?.takeIf { it.canExecute() } ?: installed.takeIf { it.canExecute() }

    override val isSupported: Boolean get() = asset != null || cli() != null

    override suspend fun unavailableReason(): String? = withContext(Dispatchers.IO) {
        when {
            cli() != null -> null
            asset != null -> "Install the translator (${asset.sizeLabel})"
            else -> bergamotUnsupportedReason(platformKey)
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
                    // Pipes are written and drained concurrently so neither side can block the other.
                    val out = async { process.inputStream.readBytes().decodeToString() }
                    val err = async { process.errorStream.readBytes().decodeToString() }
                    val write = async { process.outputStream.use { it.write(text.toByteArray()) } }
                    try {
                        val exited = runInterruptible { process.waitFor(timeoutSeconds, TimeUnit.SECONDS) }
                        if (!exited) throw IOException("Translation timed out")
                    } catch (t: Throwable) {
                        // Timeout, cancellation or failure: kill first, because the pipe jobs above only finish once
                        // the process is gone and coroutineScope waits for them.
                        process.destroyForcibly()
                        throw t
                    }
                    write.await()
                    val stdout = out.await()
                    val stderr = err.await()
                    if (process.exitValue() != 0) {
                        throw IOException("Translator exited with ${process.exitValue()}: ${stderr.trim().takeLast(300)}")
                    }
                    spaceBetweenSentences(stdout.trim())
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
