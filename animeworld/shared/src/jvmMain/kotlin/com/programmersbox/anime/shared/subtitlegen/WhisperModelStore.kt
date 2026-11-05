package com.programmersbox.anime.shared.subtitlegen

import com.programmersbox.anime.shared.AnimeDesktopSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

sealed interface ModelDownload {
    data object Missing : ModelDownload
    data class Downloading(val fraction: Float, val bytes: Long) : ModelDownload
    /** The last download failed; [message] is a short reason. Calling `download()` again retries. */
    data class Failed(val message: String) : ModelDownload
    data object Ready : ModelDownload
}

data class ModelRow(
    val choice: WhisperModelChoice,
    val name: String,
    val sizeLabel: String,
    val download: ModelDownload,
    val selected: Boolean,
    /** Absolute path; custom models only. */
    val path: String?,
)

sealed interface AddModelResult {
    data object Added : AddModelResult
    data class Rejected(val message: String) : AddModelResult
}

class ModelMissingException(message: String) : Exception(message)

/**
 * The whisper models the user can pick: built-ins downloaded into [modelsDir] (as `<name>.part`, renamed on
 * success) and the user's own `.bin` files, which are referenced by path and never deleted.
 */
class WhisperModelStore(
    settings: AnimeDesktopSettings,
    private val modelsDir: File,
    private val client: OkHttpClient,
    private val scope: CoroutineScope,
    private val baseUrl: String = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/",
) {
    private data class State(
        val selected: WhisperModelChoice,
        val custom: List<String>,
        val downloading: Map<BuiltInWhisperModel, ModelDownload>,
    )

    private val lock = Any()
    private val jobs = mutableMapOf<BuiltInWhisperModel, Job>()
    /** Jobs cancelled by [delete]; a new download waits for them before reusing the `.part`. */
    private val retired = mutableMapOf<BuiltInWhisperModel, Job>()

    private var state: State = runBlocking {
        State(
            selected = decodeModelChoice(settings.whisperModel.get()),
            custom = settings.customWhisperModels.get().toList(),
            downloading = emptyMap(),
        )
    }

    private val _rows = MutableStateFlow(buildRows(state))
    val rows: StateFlow<List<ModelRow>> = _rows

    private val _summary = MutableStateFlow(buildSummary(state))
    val summary: StateFlow<ModelSummary> = _summary

    private val _downloadedBytes = MutableStateFlow(readyBytes())
    val downloadedBytes: StateFlow<Long> = _downloadedBytes

    // Conflated writers: the last value wins, so quick successive changes persist in order.
    private val selectedPersist = MutableStateFlow(state.selected.encode())
    private val customPersist = MutableStateFlow(state.custom)

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { selectedPersist.drop(1).collect { settings.whisperModel.set(it) } }
        scope.launch(start = CoroutineStart.UNDISPATCHED) { customPersist.drop(1).collect { settings.customWhisperModels.set(it.toCollection(LinkedHashSet())) } }
    }

    fun select(choice: WhisperModelChoice) = update { it.copy(selected = choice) }

    fun download(model: BuiltInWhisperModel) {
        startDownload(model)
    }

    fun delete(model: BuiltInWhisperModel) = synchronized(lock) {
        // Dropping the job from [jobs] makes it a non-current job: it stops without touching state or files.
        jobs.remove(model)?.also { it.cancel(); retired[model] = it }
        File(modelsDir, model.fileName).delete()
        File(modelsDir, model.fileName + ".part").delete()
        update { it.copy(downloading = it.downloading - model) }
    }

    suspend fun addCustom(file: File): AddModelResult {
        val rejected = AddModelResult.Rejected("${file.name} isn't a whisper.cpp model. Pick a ggml .bin file.")
        val header = withContext(Dispatchers.IO) {
            runCatching { file.inputStream().use { it.readNBytes(4) } }.getOrNull()
        }
        if (header == null || !isWhisperModelHeader(header)) return rejected
        val path = file.absolutePath
        update { s -> s.copy(custom = if (path in s.custom) s.custom else s.custom + path, selected = WhisperModelChoice.Custom(path)) }
        return AddModelResult.Added
    }

    fun removeCustom(path: String) = update { s ->
        val removedSelected = (s.selected as? WhisperModelChoice.Custom)?.path == path
        s.copy(
            custom = s.custom - path,
            selected = if (removedSelected) WhisperModelChoice.BuiltIn(BuiltInWhisperModel.SMALL) else s.selected,
        )
    }

    /** The selected model's file, downloading a missing built-in first. */
    suspend fun ensureSelected(onProgress: (Float) -> Unit): File {
        when (val choice = state.selected) {
            is WhisperModelChoice.Custom -> {
                val file = File(choice.path)
                if (!file.isFile) throw ModelMissingException("Model file not found")
                return file
            }

            is WhisperModelChoice.BuiltIn -> {
                val file = File(modelsDir, choice.model.fileName)
                if (file.isFile) return file
                val job = startDownload(choice.model)
                coroutineScope {
                    val progress = launch {
                        _rows.collect { rows ->
                            (rows.first { it.choice == choice }.download as? ModelDownload.Downloading)?.let { onProgress(it.fraction) }
                        }
                    }
                    job.join()
                    progress.cancel()
                }
                if (!file.isFile) {
                    val failed = state.downloading[choice.model] as? ModelDownload.Failed
                    throw IOException(failed?.message ?: "Download of ${choice.model.fileName} was cancelled")
                }
                onProgress(1f)
                return file
            }
        }
    }

    private fun startDownload(model: BuiltInWhisperModel): Job = synchronized(lock) {
        jobs[model]?.takeIf { it.isActive }?.let { return it }
        val previous = jobs[model] ?: retired[model]
        update { it.copy(downloading = it.downloading + (model to ModelDownload.Downloading(0f, 0L))) }
        scope.launch(Dispatchers.IO) {
            val self = currentCoroutineContext()[Job]
            val target = File(modelsDir, model.fileName)
            val part = File(modelsDir, model.fileName + ".part")
            // Only the current job for this model may touch its state or .part; a deleted job just stops.
            fun isCurrent() = synchronized(lock) { jobs[model] === self }
            var failure: String? = null
            try {
                // A cancelled predecessor (delete then download) must be done with the .part before we reuse it.
                previous?.join()
                modelsDir.mkdirs()
                client.newCall(Request.Builder().url(baseUrl + model.fileName).build()).execute().use { response ->
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
                                written += read
                                val fraction = if (total > 0) (written.toFloat() / total).coerceIn(0f, 1f) else 0f
                                // Throttle: publish when the percent changes or 100 ms passed.
                                val percent = (fraction * 100).toInt()
                                val now = System.nanoTime()
                                if (percent != lastPercent || now - lastAt >= 100_000_000L) {
                                    lastPercent = percent
                                    lastAt = now
                                    // A deleted (non-current) job must not bring the row back to Downloading.
                                    synchronized(lock) {
                                        if (jobs[model] === self) {
                                            update { it.copy(downloading = it.downloading + (model to ModelDownload.Downloading(fraction, written))) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                target.delete()
                if (!part.renameTo(target)) throw IOException("Couldn't move ${part.name} into place")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = "Download failed: " + (e.message ?: e::class.simpleName)
            } finally {
                if (isCurrent()) {
                    part.delete()
                    update {
                        it.copy(downloading = if (failure != null) it.downloading + (model to ModelDownload.Failed(failure)) else it.downloading - model)
                    }
                }
            }
        }.also { jobs[model] = it }
    }

    private fun update(block: (State) -> State) = synchronized(lock) {
        val next = block(state)
        state = next
        _rows.value = buildRows(next)
        _summary.value = buildSummary(next)
        _downloadedBytes.value = readyBytes()
        selectedPersist.value = next.selected.encode()
        customPersist.value = next.custom
    }

    private fun builtInDownload(model: BuiltInWhisperModel, s: State): ModelDownload =
        s.downloading[model] ?: if (File(modelsDir, model.fileName).isFile) ModelDownload.Ready else ModelDownload.Missing

    private fun buildRows(s: State): List<ModelRow> =
        BuiltInWhisperModel.entries.map { model ->
            val choice = WhisperModelChoice.BuiltIn(model)
            ModelRow(choice, model.label, model.sizeLabel, builtInDownload(model, s), s.selected == choice, null)
        } + s.custom.map { path ->
            val file = File(path)
            val choice = WhisperModelChoice.Custom(path)
            ModelRow(
                choice = choice,
                name = customModelName(file.name),
                sizeLabel = if (file.isFile) formatSize(file.length()) else "Missing file",
                download = ModelDownload.Ready,
                selected = s.selected == choice,
                path = path,
            )
        }

    private fun buildSummary(s: State): ModelSummary = when (val choice = s.selected) {
        is WhisperModelChoice.BuiltIn -> ModelSummary(
            name = choice.model.label,
            downloaded = builtInDownload(choice.model, s) == ModelDownload.Ready,
            sizeLabel = choice.model.sizeLabel,
            translatesWell = !choice.model.sameLanguageOnly,
            englishOnly = isEnglishOnlyModel(choice.model.fileName),
        )

        is WhisperModelChoice.Custom -> {
            val file = File(choice.path)
            ModelSummary(
                name = customModelName(file.name),
                downloaded = file.isFile,
                sizeLabel = if (file.isFile) formatSize(file.length()) else "Missing file",
                translatesWell = true,
                englishOnly = isEnglishOnlyModel(file.name),
                missing = !file.isFile,
            )
        }
    }

    private fun readyBytes(): Long = BuiltInWhisperModel.entries.sumOf { model ->
        File(modelsDir, model.fileName).takeIf { it.isFile && state.downloading[model] == null }?.length() ?: 0L
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / 1_000_000.0
        return if (mb < 1000) "${mb.toLong()} MB" else "%.1f GB".format(java.util.Locale.US, mb / 1000)
    }
}
