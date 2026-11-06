package com.programmersbox.kmpuiviews.translation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.EngineKind
import com.programmersbox.kmpuiviews.translation.LanguageDetector
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotModelStore
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotRow
import com.programmersbox.kmpuiviews.translation.bergamot.BergamotRuntime
import com.programmersbox.kmpuiviews.translation.custom.FolderCheck
import com.programmersbox.kmpuiviews.translation.custom.OpenAiCompatClient
import com.programmersbox.kmpuiviews.translation.custom.inspectFolder
import com.programmersbox.kmpuiviews.translation.ollama.OllamaClient
import com.programmersbox.kmpuiviews.translation.ollama.OllamaModel
import com.programmersbox.kmpuiviews.translation.ollama.OllamaStatus
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.TimeSource

data class CustomFolder(val path: String, val check: FolderCheck)

/** An Ollama pull in progress; [fraction] is null while Ollama doesn't report a total. */
data class OllamaPull(val name: String, val fraction: Float?)

sealed interface ServerCheck {
    data object Checking : ServerCheck
    data class Connected(val models: Int) : ServerCheck
    data class Failed(val message: String) : ServerCheck
}

data class DesktopTranslationState(
    val target: String = "en",
    val targets: List<String> = listOf("en"),
    val engine: EngineKind = EngineKind.Bergamot,
    // Bergamot
    val bergamotRows: List<BergamotRow> = emptyList(),
    val bergamotBytes: Long = 0,
    val bergamotPairs: Int = 0,
    val bergamotIndexError: String? = null,
    val runtimeIssue: String? = null,
    /** Non-null while the translator is being installed. */
    val runtimeInstall: Float? = null,
    // Ollama
    val ollamaUrl: String = "",
    /** Null until the first check finishes. */
    val ollamaStatus: OllamaStatus? = null,
    val ollamaChecking: Boolean = false,
    val ollamaModel: String = "",
    val pullName: String = "",
    val pull: OllamaPull? = null,
    // Custom
    val customMode: String = CUSTOM_FOLDER,
    val customFolders: List<CustomFolder> = emptyList(),
    val serverUrl: String = "",
    val serverModel: String = "",
    val serverKey: String = "",
    val serverCheck: ServerCheck? = null,
    // Try it
    val testInput: String = SAMPLE_TEXT,
    val detectedSource: String? = null,
    val testOutput: String? = null,
    val testTiming: String? = null,
    val testError: String? = null,
    val testRunning: Boolean = false,
    /** [bergamotDeleteId] or [ollamaDeleteId] of the row showing its delete confirmation. */
    val confirmingId: String? = null,
) {
    val ollamaModels: List<OllamaModel> get() = (ollamaStatus as? OllamaStatus.Connected)?.models.orEmpty()

    /** Bytes the selected engine keeps on disk; null when the app can't count them. */
    val storageBytes: Long?
        get() = when (engine) {
            EngineKind.Bergamot -> bergamotBytes
            EngineKind.Ollama -> (ollamaStatus as? OllamaStatus.Connected)?.models?.sumOf { it.sizeBytes }
            EngineKind.Custom -> null
        }

    val storageLabel: String
        get() = when (engine) {
            EngineKind.Bergamot -> if (bergamotPairs == 1) "1 language pair" else "$bergamotPairs language pairs"
            EngineKind.Ollama -> "Ollama models (managed by Ollama)"
            EngineKind.Custom -> "Your files aren't counted"
        }
}

const val CUSTOM_FOLDER = "folder"
const val CUSTOM_SERVER = "server"
private const val SAMPLE_TEXT = "彼は静かに剣を抜き、雨の中へ踏み出した。"

fun bergamotDeleteId(source: String) = "bergamot:$source"
fun ollamaDeleteId(name: String) = "ollama:$name"

internal fun languageName(code: String) = TranslationLanguages.byCode(code)?.englishName ?: code

/** "84 ms" under a second, else seconds with one decimal ("1.9 s"). */
internal fun formatElapsed(millis: Long): String =
    if (millis < 1000) "$millis ms" else "${millis / 1000}.${(millis % 1000) / 100} s"

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopTranslationViewModel(
    private val settings: DesktopTranslationSettings,
    private val appSettings: NewSettingsHandling,
    private val engines: Map<EngineKind, TranslationEngine>,
    private val store: BergamotModelStore,
    private val runtime: BergamotRuntime,
    private val ollama: OllamaClient,
    private val server: OpenAiCompatClient,
    private val detector: LanguageDetector,
    private val notifier: InAppNotifier,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {

    private val _state = MutableStateFlow(DesktopTranslationState())
    val state: StateFlow<DesktopTranslationState> = _state.asStateFlow()

    private var pullJob: Job? = null

    private val engineFlow: Flow<EngineKind> = settings.engine.asFlow()
        .map { runCatching { EngineKind.valueOf(it) }.getOrDefault(EngineKind.Bergamot) }
        .distinctUntilChanged()

    init {
        engineFlow.onEach { kind -> _state.update { it.copy(engine = kind) } }.launchIn(viewModelScope)
        appSettings.translationTargetLanguage.asFlow()
            .onEach { code -> _state.update { it.copy(target = code) } }
            .launchIn(viewModelScope)
        settings.ollamaModel.asFlow().onEach { m -> _state.update { it.copy(ollamaModel = m) } }.launchIn(viewModelScope)
        settings.customMode.asFlow().onEach { m -> _state.update { it.copy(customMode = m) } }.launchIn(viewModelScope)
        settings.customFolders.asFlow()
            .map { paths -> paths.sorted().map { CustomFolder(it, inspectFolder(File(it))) } }
            .flowOn(Dispatchers.IO)
            .onEach { folders -> _state.update { it.copy(customFolders = folders) } }
            .launchIn(viewModelScope)

        store.rows.onEach { rows -> _state.update { it.copy(bergamotRows = rows) } }.launchIn(viewModelScope)
        store.indexError.onEach { e -> _state.update { it.copy(bergamotIndexError = e) } }.launchIn(viewModelScope)
        store.totalBytes
            .onEach { bytes -> _state.update { it.copy(bergamotBytes = bytes, bergamotPairs = store.installedPairs().size) } }
            .launchIn(viewModelScope)

        combine(
            engineFlow,
            store.rows,
            settings.ollamaModel.asFlow(),
            settings.customFolders.asFlow(),
            combine(settings.customMode.asFlow(), settings.customServerModel.asFlow(), appSettings.translationTargetLanguage.asFlow()) { a, b, c -> Triple(a, b, c) },
        ) { kind, rows, _, _, triple -> Triple(kind, rows, triple.third) }
            .mapLatest { (kind, rows, target) -> computeTargets(kind, rows, target) }
            .onEach { list -> _state.update { it.copy(targets = list) } }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            _state.update {
                it.copy(
                    ollamaUrl = settings.ollamaUrl.get(),
                    serverUrl = settings.customServerUrl.get(),
                    serverModel = settings.customServerModel.get(),
                    serverKey = settings.customServerKey.get(),
                )
            }
            checkOllama()
        }
        viewModelScope.launch {
            val reason = runCatching { runtime.unavailableReason() }.getOrElse { it.message }
            _state.update { it.copy(runtimeIssue = reason) }
        }
    }

    /** The engine's targets plus the current one; Bergamot also offers every language its model list covers. */
    private suspend fun computeTargets(kind: EngineKind, rows: List<BergamotRow>, target: String): List<String> {
        val fromEngine = runCatching { engines[kind]?.targets().orEmpty() }.getOrDefault(emptyList())
        val fromIndex = if (kind == EngineKind.Bergamot) rows.flatMap { r -> r.chain.flatMap { listOf(it.from, it.to) } } else emptyList()
        return (fromEngine + fromIndex + target).distinct().sortedBy(::languageName)
    }

    // ---------- top ----------

    fun setTarget(code: String) {
        _state.update { it.copy(target = code, testOutput = null, testTiming = null, testError = null) }
        viewModelScope.launch { appSettings.translationTargetLanguage.set(code) }
    }

    fun setEngine(kind: EngineKind) {
        if (_state.value.engine == kind) return
        _state.update { it.copy(engine = kind, confirmingId = null, testOutput = null, testTiming = null, testError = null) }
        viewModelScope.launch { settings.engine.set(kind.name) }
        if (kind == EngineKind.Ollama) viewModelScope.launch { checkOllama() }
    }

    // ---------- delete confirmation ----------

    fun askDelete(id: String) = _state.update { it.copy(confirmingId = id) }

    fun cancelDelete() = _state.update { it.copy(confirmingId = null) }

    fun confirmDelete() {
        val id = _state.value.confirmingId ?: return
        _state.update { it.copy(confirmingId = null) }
        viewModelScope.launch {
            when {
                id.startsWith("bergamot:") -> {
                    val source = id.removePrefix("bergamot:")
                    runAction("Couldn't delete ${languageName(source)}") { store.delete(source) }
                }

                id.startsWith("ollama:") -> deleteOllamaModel(id.removePrefix("ollama:"))
            }
        }
    }

    // ---------- Bergamot ----------

    fun download(source: String) = store.download(source)

    fun cancelDownload(source: String) = store.cancel(source)

    fun retryIndex() {
        viewModelScope.launch { store.refresh() }
    }

    fun installRuntime() {
        if (_state.value.runtimeInstall != null) return
        _state.update { it.copy(runtimeInstall = 0f) }
        viewModelScope.launch {
            try {
                runAction("Couldn't install the translator") {
                    runtime.install { p -> _state.update { it.copy(runtimeInstall = p) } }
                }
                val reason = runCatching { runtime.unavailableReason() }.getOrElse { it.message }
                _state.update { it.copy(runtimeIssue = reason) }
            } finally {
                _state.update { it.copy(runtimeInstall = null) }
            }
        }
    }

    // ---------- Ollama ----------

    fun setOllamaUrl(url: String) {
        _state.update { it.copy(ollamaUrl = url) }
        viewModelScope.launch { settings.ollamaUrl.set(url) }
    }

    fun checkOllama() {
        viewModelScope.launch {
            settings.ollamaUrl.set(_state.value.ollamaUrl.ifBlank { settings.ollamaUrl.get() })
            refreshOllama()
        }
    }

    private suspend fun refreshOllama() {
        _state.update { it.copy(ollamaChecking = true) }
        val status = ollama.status()
        _state.update { it.copy(ollamaStatus = status, ollamaChecking = false) }
    }

    fun selectOllamaModel(name: String) {
        _state.update { it.copy(ollamaModel = name) }
        viewModelScope.launch { settings.ollamaModel.set(name) }
    }

    fun setPullName(name: String) = _state.update { it.copy(pullName = name) }

    fun pullModel() {
        val name = _state.value.pullName.trim()
        if (name.isEmpty() || pullJob?.isActive == true) return
        _state.update { it.copy(pull = OllamaPull(name, null)) }
        pullJob = viewModelScope.launch {
            try {
                ollama.pull(name).collect { f -> _state.update { it.copy(pull = OllamaPull(name, f)) } }
                _state.update { it.copy(pullName = "") }
                if (settings.ollamaModel.get().isBlank()) selectOllamaModel(name)
                refreshOllama()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                notifier.error(title = "Couldn't pull $name", subtitle = e.message)
            } finally {
                _state.update { it.copy(pull = null) }
            }
        }
    }

    fun cancelPull() {
        pullJob?.cancel()
    }

    private suspend fun deleteOllamaModel(name: String) {
        val before = _state.value.ollamaModels.map { it.name }
        runAction("Couldn't delete $name") {
            ollama.delete(name)
            if (settings.ollamaModel.get() == name) {
                val index = before.indexOf(name)
                val next = before.drop(index + 1).firstOrNull() ?: before.take(index.coerceAtLeast(0)).lastOrNull()
                _state.update { it.copy(ollamaModel = next.orEmpty()) }
                settings.ollamaModel.set(next.orEmpty())
            }
        }
        refreshOllama()
    }

    // ---------- Custom ----------

    fun setCustomMode(mode: String) {
        _state.update { it.copy(customMode = mode) }
        viewModelScope.launch { settings.customMode.set(mode) }
    }

    /** Adds [path] to the list; the folder itself is never touched. */
    fun addFolder(path: String) {
        viewModelScope.launch { settings.customFolders.set(settings.customFolders.get() + path) }
    }

    /** Removes [path] from the list only; nothing is deleted from disk. */
    fun removeFolder(path: String) {
        viewModelScope.launch { settings.customFolders.set(settings.customFolders.get() - path) }
    }

    fun setServerUrl(value: String) {
        _state.update { it.copy(serverUrl = value, serverCheck = null) }
        viewModelScope.launch { settings.customServerUrl.set(value) }
    }

    fun setServerModel(value: String) {
        _state.update { it.copy(serverModel = value) }
        viewModelScope.launch { settings.customServerModel.set(value) }
    }

    fun setServerKey(value: String) {
        _state.update { it.copy(serverKey = value, serverCheck = null) }
        viewModelScope.launch { settings.customServerKey.set(value) }
    }

    fun checkServer() {
        _state.update { it.copy(serverCheck = ServerCheck.Checking) }
        viewModelScope.launch {
            settings.customServerUrl.set(_state.value.serverUrl)
            settings.customServerKey.set(_state.value.serverKey)
            val result = server.check().fold(
                onSuccess = { ServerCheck.Connected(it) },
                onFailure = { ServerCheck.Failed(it.message ?: "Couldn't reach the server") },
            )
            _state.update { it.copy(serverCheck = result) }
        }
    }

    // ---------- Try it ----------

    fun setTestInput(text: String) = _state.update { it.copy(testInput = text) }

    fun runTest() {
        if (_state.value.testRunning) return
        _state.update { it.copy(testRunning = true, testOutput = null, testTiming = null, testError = null) }
        viewModelScope.launch {
            val s = _state.value
            try {
                val source = withContext(Dispatchers.Default) { detector.detect(s.testInput) }
                _state.update { it.copy(detectedSource = source) }
                when {
                    source == null -> fail("Couldn't tell what language this is")
                    source == s.target -> _state.update {
                        it.copy(testOutput = s.testInput, testTiming = "Already in ${languageName(s.target)}")
                    }

                    else -> {
                        val engine = engines[s.engine] ?: error("This engine isn't available")
                        val mark = timeSource.markNow()
                        val out = engine.translate(s.testInput, source, s.target)
                        val elapsed = formatElapsed(mark.elapsedNow().inWholeMilliseconds)
                        _state.update {
                            it.copy(testOutput = out, testTiming = "${s.engine.name} · $source→${s.target} · $elapsed")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail(e.message ?: "Something went wrong")
            } finally {
                _state.update { it.copy(testRunning = false) }
            }
        }
    }

    private fun fail(message: String) {
        _state.update { it.copy(testError = message) }
        notifier.error(title = "Couldn't translate", subtitle = message)
    }

    private suspend fun runAction(failTitle: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            notifier.error(title = failTitle, subtitle = e.message)
        }
    }
}
