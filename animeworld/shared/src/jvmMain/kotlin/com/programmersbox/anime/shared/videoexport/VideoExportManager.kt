package com.programmersbox.anime.shared.videoexport

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.downloads.OfflineSubtitles
import com.programmersbox.anime.shared.downloads.isBaseTaken
import com.programmersbox.anime.shared.downloads.uniqueBaseName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import kotlin.io.path.createTempDirectory

sealed interface ExportState {
    /** [fraction] is null until ffmpeg reports a position against a known duration. */
    data class Running(
        val fraction: Float?,
        val positionMs: Long,
        val durationMs: Long?,
        val speed: Float?,
        val bytes: Long,
    ) : ExportState

    data class Done(val output: File, val bytes: Long, val elapsedMs: Long) : ExportState
    data class Failed(val message: String, val log: String) : ExportState
}

data class VideoExport(val source: String, val output: File, val options: ExportOptions, val state: ExportState)

/**
 * Converts downloaded episodes with the system ffmpeg, one job per source file. Jobs run on [scope], so
 * they keep going when the export screen is closed; [exports] holds running and finished ones until [clear]ed.
 * The output is written to `<name>.part` and renamed on success; the source is never touched.
 */
class VideoExportManager(
    private val scope: CoroutineScope,
    private val settings: AnimeDesktopSettings,
    private val trayState: TrayState?,
) {
    private val jobs = mutableMapOf<String, Job>()
    private val _exports = MutableStateFlow<Map<String, VideoExport>>(emptyMap())
    val exports: StateFlow<Map<String, VideoExport>> = _exports.asStateFlow()

    /**
     * The ffmpeg to run: the configured path when it exists, else Homebrew's keg-only `ffmpeg-full` (the build
     * with libass, for burning in), else the first one on PATH or a usual install dir.
     */
    suspend fun ffmpeg(): File? = withContext(Dispatchers.IO) {
        val configured = settings.ffmpegPath.get().takeIf { it.isNotBlank() }?.let(::File)
        if (configured != null && configured.canExecute()) return@withContext configured
        val exe = if (System.getProperty("os.name").startsWith("Windows")) "ffmpeg.exe" else "ffmpeg"
        val dirs = listOf("/opt/homebrew/opt/ffmpeg-full/bin", "/usr/local/opt/ffmpeg-full/bin") +
            System.getenv("PATH").orEmpty().split(File.pathSeparator) +
            listOf("/opt/homebrew/bin", "/usr/local/bin", "/usr/bin")
        dirs.filter { it.isNotBlank() }.map { File(it, exe) }.firstOrNull { it.canExecute() }
    }

    suspend fun probe(ffmpeg: File, source: File): MediaInfo? = runInterruptible(Dispatchers.IO) {
        val process = ProcessBuilder(ffmpeg.path, "-hide_banner", "-nostdin", "-i", source.path)
            .redirectErrorStream(true)
            .start()
        val text = process.inputStream.bufferedReader().readText()
        process.waitFor()
        parseProbe(text)
    }

    /** Burning in needs ffmpeg's `subtitles` filter, which builds without libass don't have. */
    suspend fun canBurnSubtitles(ffmpeg: File): Boolean = runInterruptible(Dispatchers.IO) {
        val process = ProcessBuilder(ffmpeg.path, "-hide_banner", "-filters").redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        process.waitFor()
        hasSubtitlesFilter(text)
    }

    fun subtitlesFor(source: File): List<ExportSubtitle> = OfflineSubtitles.find(source).mapNotNull { sub ->
        val file = runCatching { File(URI(sub.url)) }.getOrNull()?.takeIf { it.isFile } ?: return@mapNotNull null
        ExportSubtitle(file, sub.lang)
    }

    /** `<source name> - <FORMAT>.<ext>` in [folder], numbered when taken; never the source's own name. */
    fun outputFor(source: File, format: ExportFormat, folder: File): File {
        val base = uniqueBaseName("${source.nameWithoutExtension} - ${format.label}") { b ->
            isBaseTaken(b) { File(folder, it).exists() }
        }
        return File(folder, "$base.${format.ext}")
    }

    fun start(ffmpeg: File, source: File, options: ExportOptions, output: File, durationMs: Long?) {
        val key = source.absolutePath
        synchronized(jobs) {
            if (jobs[key]?.isActive == true) return
            publish(key, VideoExport(key, output, options, ExportState.Running(null, 0, durationMs, null, 0)))
            jobs[key] = scope.launch { run(ffmpeg, source, options, output, durationMs) }
        }
    }

    fun cancel(source: String) {
        synchronized(jobs) { jobs.remove(source)?.cancel() }
        clear(source)
    }

    /** Forgets a finished or failed export. */
    fun clear(source: String) {
        _exports.update { it - source }
    }

    private suspend fun run(ffmpeg: File, source: File, options: ExportOptions, output: File, durationMs: Long?) {
        val key = source.absolutePath
        val part = File(output.parentFile, output.name + ".part")
        val temp = createTempDirectory("animeworld-export").toFile()
        val log = ArrayDeque<String>()
        val started = System.currentTimeMillis()
        val title = output.nameWithoutExtension
        try {
            val subtitles = subtitlesFor(source)
            val burn = (options.subtitles as? SubtitleMode.BurnIn)?.let { mode ->
                val sub = subtitles.getOrNull(mode.index) ?: error("The chosen subtitle file is missing")
                sub.file.copyTo(File(temp, "burn.${sub.file.extension}"))
            }
            val args = ffmpegArgs(ffmpeg.path, source, subtitles, options, part, burn)
            val exit = runInterruptible(Dispatchers.IO) {
                val process = ProcessBuilder(args).start()
                try {
                    val stderr = Thread {
                        process.errorStream.bufferedReader().forEachLine { line ->
                            synchronized(log) {
                                log.addLast(line)
                                if (log.size > 40) log.removeFirst()
                            }
                        }
                    }.apply { isDaemon = true; start() }
                    val parser = FfmpegProgressParser()
                    process.inputStream.bufferedReader().forEachLine { line ->
                        val p = parser.line(line) ?: return@forEachLine
                        val fraction = durationMs?.takeIf { it > 0 }?.let { (p.positionMs.toFloat() / it).coerceIn(0f, 1f) }
                        update(key) { ExportState.Running(fraction, p.positionMs, durationMs, p.speed, p.bytes) }
                    }
                    val code = process.waitFor()
                    stderr.join(1_000)
                    code
                } finally {
                    process.destroy()
                }
            }
            val logText = synchronized(log) { log.joinToString("\n") }
            if (exit != 0) throw ExportFailure(logText.lineSequence().lastOrNull { it.isNotBlank() } ?: "ffmpeg exited with $exit", logText)
            if (!part.renameTo(output)) throw ExportFailure("Could not rename ${part.name} to ${output.name}", logText)
            update(key) { ExportState.Done(output, output.length(), System.currentTimeMillis() - started) }
            notify("Exported", title, isError = false)
        } catch (e: CancellationException) {
            part.delete()
            throw e
        } catch (t: Throwable) {
            part.delete()
            val failure = t as? ExportFailure
            val message = failure?.message ?: t.message ?: t::class.simpleName.orEmpty()
            update(key) { ExportState.Failed(message, failure?.log ?: t.stackTraceToString()) }
            notify("Export Failed", "$title: $message", isError = true)
        } finally {
            temp.deleteRecursively()
            synchronized(jobs) { if (jobs[key]?.isActive != true) jobs.remove(key) }
        }
    }

    private class ExportFailure(message: String, val log: String) : Exception(message)

    private fun publish(key: String, export: VideoExport) = _exports.update { it + (key to export) }

    /** Applies [state] unless the export was cancelled meanwhile. */
    private fun update(key: String, state: () -> ExportState) = _exports.update { map ->
        val current = map[key] ?: return@update map
        map + (key to current.copy(state = state()))
    }

    private suspend fun notify(title: String, message: String, isError: Boolean) {
        val tray = trayState ?: return
        withContext(Dispatchers.Main) {
            tray.sendNotification(
                Notification(
                    title = title,
                    message = message,
                    type = if (isError) Notification.Type.Error else Notification.Type.Info
                )
            )
        }
    }
}
