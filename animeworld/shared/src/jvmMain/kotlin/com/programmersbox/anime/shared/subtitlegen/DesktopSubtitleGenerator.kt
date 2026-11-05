package com.programmersbox.anime.shared.subtitlegen

import androidx.compose.ui.window.Notification
import androidx.compose.ui.window.TrayState
import com.programmersbox.anime.shared.AnimeDesktopSettings
import com.programmersbox.anime.shared.downloads.splitEpisodeTitle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory

/** Runs a command line; returns its exit code. */
fun interface CommandRunner {
    suspend fun run(args: List<String>, onLine: (String) -> Unit): Int
}

/** Runs the command with [ProcessBuilder], stdout and stderr merged into [CommandRunner.run]'s `onLine`. */
object ProcessCommandRunner : CommandRunner {
    override suspend fun run(args: List<String>, onLine: (String) -> Unit): Int {
        var process: Process? = null
        try {
            // Started on the caller's thread, not in withContext: a cancelled withContext would drop the started process.
            val started = ProcessBuilder(args).redirectErrorStream(true).start().also { process = it }
            // Reading a process pipe can't be interrupted, so lines are read on their own thread and only the
            // (interruptible) waits run here; destroying the process on cancellation closes the pipe and ends it.
            val reader = Thread {
                // Keep draining even if onLine throws, or a full pipe would stall the process.
                started.inputStream.bufferedReader().use { input ->
                    while (true) {
                        val line = try {
                            input.readLine()
                        } catch (_: IOException) {
                            null
                        } ?: break
                        try {
                            onLine(line)
                        } catch (_: Throwable) {
                        }
                    }
                }
            }.apply { isDaemon = true; start() }
            return runInterruptible(Dispatchers.IO) {
                val code = started.waitFor()
                reader.join()
                code
            }
        } finally {
            process?.destroy()
        }
    }
}

/**
 * Generates subtitles for local videos with whisper.cpp, one at a time: a single worker takes jobs from a FIFO
 * queue. Each job extracts the audio with ffmpeg into a temp dir, makes sure the selected model is on disk, runs
 * whisper-cli, and saves the result as a generated sidecar next to the video.
 */
class DesktopSubtitleGenerator(
    private val scope: CoroutineScope,
    private val settings: AnimeDesktopSettings,
    private val models: WhisperModelStore,
    private val ffmpeg: suspend () -> File?,
    /** Downloads whisper.cpp when nothing else is found, on platforms that have a build for it. */
    installer: WhisperCliInstaller? = null,
    /** Finds whisper-cli, downloading it if needed while reporting the download's progress. */
    private val whisper: suspend (onProgress: (Float) -> Unit) -> File? = { onProgress -> locateWhisperCli(settings, installer, onProgress) },
    private val runner: CommandRunner = ProcessCommandRunner,
    private val onSidecarsChanged: () -> Unit,
    private val trayState: TrayState?,
    private val isMac: Boolean = System.getProperty("os.name").startsWith("Mac"),
) : SubtitleGenerator {

    private class Entry(val path: String, val request: GenerationRequest) {
        /** Set under [lock] once the job is cancelled; its status must never be written again. */
        var cancelled = false
        var job: Job? = null
    }

    private val lock = Any()
    private val pending = LinkedHashMap<String, Entry>()
    private var active: Entry? = null
    private val queue = Channel<Entry>(Channel.UNLIMITED)

    private val _statuses = MutableStateFlow<Map<String, GenerationStatus>>(emptyMap())
    override val statuses: StateFlow<Map<String, GenerationStatus>> = _statuses.asStateFlow()

    private val _finished = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val finished: SharedFlow<String> = _finished.asSharedFlow()

    override val model: StateFlow<ModelSummary> = models.summary

    override val lastRequest: StateFlow<GenerationRequest> =
        combine(settings.generateFrom.asFlow(), settings.generateTo.asFlow(), ::requestOf)
            .stateIn(scope, SharingStarted.Eagerly, requestOf("ja", "en"))

    init {
        scope.launch {
            for (entry in queue) {
                val job = synchronized(lock) {
                    if (pending[entry.path] !== entry) return@synchronized null
                    pending.remove(entry.path)
                    active = entry
                    _statuses.update { it + (entry.path to GenerationStatus.ExtractingAudio) }
                    relabelQueued()
                    // A child of the worker: cancelling it ends only this job, and join() waits for its cleanup.
                    launch {
                        // Nothing may escape a job: a failure here would cancel the worker and stall the queue for good.
                        try {
                            run(entry)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (t: Throwable) {
                            set(entry, GenerationStatus.Failed(t.message ?: t::class.simpleName.orEmpty(), entry.request))
                        }
                    }.also { entry.job = it }
                } ?: continue
                job.join()
                synchronized(lock) { if (active === entry) active = null }
            }
        }
    }

    override fun enqueue(videoPath: String, request: GenerationRequest, remember: Boolean) {
        if (remember) scope.launch {
            runCatching {
                settings.generateFrom.set(request.from.code)
                settings.generateTo.set(request.to.code)
            }
        }
        synchronized(lock) {
            // A cancelled job may still be cleaning up; the same video can be queued again meanwhile.
            val running = active?.takeIf { !it.cancelled }
            if (videoPath in pending || running?.path == videoPath) return
            val ahead = pending.values.lastOrNull() ?: running
            val entry = Entry(videoPath, request)
            pending[videoPath] = entry
            _statuses.update {
                it + (videoPath to if (ahead == null) GenerationStatus.ExtractingAudio else GenerationStatus.Queued(titleOf(ahead.path)))
            }
            queue.trySend(entry)
        }
    }

    override fun cancel(videoPath: String) {
        synchronized(lock) {
            val entry = pending.remove(videoPath) ?: active?.takeIf { it.path == videoPath } ?: return
            entry.cancelled = true
            entry.job?.cancel()
            _statuses.update { it - videoPath }
            relabelQueued()
        }
    }

    /** Re-titles each queued job after the one immediately ahead of it. Call with [lock] held. */
    private fun relabelQueued() {
        var ahead: Entry? = active?.takeIf { !it.cancelled }
        val updates = HashMap<String, GenerationStatus>()
        for (entry in pending.values) {
            if (ahead != null) updates[entry.path] = GenerationStatus.Queued(titleOf(ahead.path))
            ahead = entry
        }
        if (updates.isNotEmpty()) _statuses.update { it + updates }
    }

    override fun dismiss(videoPath: String) = _statuses.update { map ->
        if (map[videoPath] is GenerationStatus.Failed) map - videoPath else map
    }

    private suspend fun run(entry: Entry) {
        val title = File(entry.path).nameWithoutExtension
        var temp: File? = null
        val failure: String? = try {
            generate(entry, createTempDirectory("animeworld-subgen").toFile().also { temp = it })
            null
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            t.message ?: t::class.simpleName.orEmpty()
        } finally {
            temp?.deleteRecursively()
        }
        if (failure == null) {
            // The track is saved and the status cleared; listeners failing must not turn that into a failure.
            _finished.tryEmit(entry.path)
            quietly { onSidecarsChanged() }
            quietly { notify("Subtitles ready", title, isError = false) }
        } else {
            set(entry, GenerationStatus.Failed(failure, entry.request))
            quietly { notify("Subtitles Failed", "$title: $failure", isError = true) }
        }
    }

    /** Extracts, transcribes and saves the track in [temp]; returns once the sidecar is saved and the status cleared. */
    private suspend fun generate(entry: Entry, temp: File) {
        val video = File(entry.path)
        val ffmpeg = ffmpeg() ?: throw GenerationFailure("ffmpeg not found. Install it or set its location in Video export.")
        var downloaded = false
        val whisper = try {
            whisper { fraction ->
                downloaded = true
                set(entry, GenerationStatus.DownloadingModel("whisper.cpp", fraction))
            }
        } catch (e: WhisperCliDownloadException) {
            throw GenerationFailure("Couldn't download whisper.cpp: ${e.message}")
        } ?: throw GenerationFailure(
            if (isMac) "whisper-cli not found. Install it with brew install whisper-cpp, or set its location in Settings › Subtitles."
            else "whisper-cli not found. Set its location in Settings › Subtitles."
        )
        if (downloaded) set(entry, GenerationStatus.ExtractingAudio)

        val wav = File(temp, "audio.wav")
        exec(extractAudioArgs(ffmpeg.path, video, wav), "ffmpeg")

        val model = models.ensureSelected { fraction ->
            set(entry, GenerationStatus.DownloadingModel(models.summary.value.name, fraction))
        }

        val (from, to) = entry.request
        set(entry, GenerationStatus.Transcribing(from, to, 0f, null))
        val outBase = File(temp, "subtitles")
        val started = System.currentTimeMillis()
        exec(whisperArgs(whisper.path, model, wav, outBase, entry.request), "whisper-cli") { line ->
            val percent = parseWhisperProgress(line) ?: return@exec
            val fraction = percent / 100f
            set(entry, GenerationStatus.Transcribing(from, to, fraction, remainingMs(System.currentTimeMillis() - started, fraction)))
        }
        val vtt = File(outBase.path + ".vtt")
        if (!vtt.isFile) throw GenerationFailure("whisper-cli didn't write any subtitles")

        // A job cancelled up to here leaves nothing behind. Saving holds the lock so cancel() can't land midway.
        currentCoroutineContext().ensureActive()
        val language = if (to.code == "auto") WhisperLanguage("auto", "Original") else to
        synchronized(lock) {
            if (entry.cancelled) throw CancellationException("Cancelled")
            if (!video.isFile) throw GenerationFailure("The episode was deleted.")
            GeneratedSidecars.add(video, vtt, language)
            _statuses.update { it - entry.path }
        }
    }

    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
        }
    }

    /** Runs [args]; a non-zero exit fails with the last non-blank output line. */
    private suspend fun exec(args: List<String>, name: String, onLine: (String) -> Unit = {}) {
        val tail = ArrayDeque<String>()
        val exit = runner.run(args) { line ->
            synchronized(tail) {
                if (line.isNotBlank()) tail.addLast(line)
                if (tail.size > 20) tail.removeFirst()
            }
            onLine(line)
        }
        if (exit != 0) throw GenerationFailure(synchronized(tail) { tail.lastOrNull() } ?: "$name exited with code $exit")
    }

    /** Applies [status] unless the job was cancelled meanwhile. */
    private fun set(entry: Entry, status: GenerationStatus) = synchronized(lock) {
        if (!entry.cancelled) _statuses.update { it + (entry.path to status) }
    }

    private class GenerationFailure(message: String) : Exception(message)

    private fun titleOf(path: String): String {
        val name = File(path).nameWithoutExtension
        return splitEpisodeTitle(name).episode ?: name
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

private fun requestOf(from: String, to: String): GenerationRequest = GenerationRequest(
    from = if (from == WhisperAuto.code) WhisperAuto else whisperLanguages.firstOrNull { it.code == from } ?: whisperLanguages.first { it.code == "ja" },
    to = if (to == "auto") WhisperLanguage("auto", "Same as spoken") else whisperLanguages.firstOrNull { it.code == to } ?: whisperLanguages.first { it.code == "en" },
)
