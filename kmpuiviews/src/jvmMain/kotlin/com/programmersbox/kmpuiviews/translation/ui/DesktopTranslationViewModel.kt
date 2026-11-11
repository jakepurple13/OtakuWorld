package com.programmersbox.kmpuiviews.translation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.kmpuiviews.domain.TranslationLanguages
import com.programmersbox.kmpuiviews.translation.DesktopTranslationSettings
import com.programmersbox.kmpuiviews.translation.EngineKind
import com.programmersbox.kmpuiviews.translation.LanguageDetector
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.custom.OpenAiCompatClient
import com.programmersbox.kmpuiviews.translation.ollama.OllamaClient
import com.programmersbox.kmpuiviews.translation.ollama.OllamaModel
import com.programmersbox.kmpuiviews.translation.ollama.OllamaStatus
import com.programmersbox.sharedcomponents.notifier.InAppNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource

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
    val engine: EngineKind = EngineKind.Ollama,
    // Ollama
    val ollamaUrl: String = "",
    /** Null until the first check finishes. */
    val ollamaStatus: OllamaStatus? = null,
    val ollamaChecking: Boolean = false,
    val ollamaModel: String = "",
    val pullName: String = "",
    val pull: OllamaPull? = null,
    // Custom (OpenAI-compatible server)
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
    /** [ollamaDeleteId] of the row showing its delete confirmation. */
    val confirmingId: String? = null,
) {
    val ollamaModels: List<OllamaModel> get() = (ollamaStatus as? OllamaStatus.Connected)?.models.orEmpty()

    /** Bytes the selected engine keeps on disk; null when the app can't count them. */
    val storageBytes: Long?
        get() = when (engine) {
            EngineKind.Ollama -> (ollamaStatus as? OllamaStatus.Connected)?.models?.sumOf { it.sizeBytes }
            EngineKind.Custom -> null
        }

    val storageLabel: String
        get() = when (engine) {
            EngineKind.Ollama -> "Ollama models (managed by Ollama)"
            EngineKind.Custom -> "Your server's models aren't counted"
        }
}

private const val SAMPLE_TEXT = "彼は静かに剣を抜き、雨の中へ踏み出した。"

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
    private val ollama: OllamaClient,
    private val server: OpenAiCompatClient,
    private val detector: LanguageDetector,
    private val notifier: InAppNotifier,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {

    private val _state = MutableStateFlow(DesktopTranslationState())
    val state: StateFlow<DesktopTranslationState> = _state.asStateFlow()

    private var pullJob: Job? = null
    private var testJob: Job? = null

    /** An unknown stored name (e.g. the removed "Bergamot") reads as Ollama, like [DesktopTranslationHandler]. */
    private val engineFlow: Flow<EngineKind> = settings.engine.asFlow()
        .map { runCatching { EngineKind.valueOf(it) }.getOrDefault(EngineKind.Ollama) }
        .distinctUntilChanged()

    init {
        engineFlow.onEach { kind -> _state.update { it.copy(engine = kind) } }.launchIn(viewModelScope)
        appSettings.translationTargetLanguage.asFlow()
            .onEach { code -> _state.update { it.copy(target = code) } }
            .launchIn(viewModelScope)
        settings.ollamaModel.asFlow().onEach { m -> _state.update { it.copy(ollamaModel = m) } }.launchIn(viewModelScope)

        combine(
            engineFlow,
            settings.ollamaModel.asFlow(),
            settings.customServerModel.asFlow(),
            appSettings.translationTargetLanguage.asFlow(),
        ) { kind, _, _, target -> kind to target }
            .mapLatest { (kind, target) -> computeTargets(kind, target) }
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
    }

    /** The engine's targets plus the current one, so the dropdown always shows what's selected. */
    private suspend fun computeTargets(kind: EngineKind, target: String): List<String> {
        val fromEngine = runCatching { engines[kind]?.targets().orEmpty() }.getOrDefault(emptyList())
        return (fromEngine + target).distinct().sortedBy(::languageName)
    }

    // ---------- top ----------

    fun setTarget(code: String) {
        cancelTest()
        _state.update { it.copy(target = code) }
        viewModelScope.launch { appSettings.translationTargetLanguage.set(code) }
    }

    fun setEngine(kind: EngineKind) {
        if (_state.value.engine == kind) return
        cancelTest()
        _state.update { it.copy(engine = kind, confirmingId = null) }
        viewModelScope.launch { settings.engine.set(kind.name) }
        if (kind == EngineKind.Ollama) viewModelScope.launch { checkOllama() }
    }

    // ---------- delete confirmation ----------

    fun askDelete(id: String) = _state.update { it.copy(confirmingId = id) }

    fun cancelDelete() = _state.update { it.copy(confirmingId = null) }

    fun confirmDelete() {
        val id = _state.value.confirmingId ?: return
        _state.update { it.copy(confirmingId = null) }
        if (id.startsWith("ollama:")) viewModelScope.launch { deleteOllamaModel(id.removePrefix("ollama:")) }
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

    /** Stops a running test and clears its result, so it never shows under another engine or target. */
    private fun cancelTest() {
        testJob?.cancel()
        testJob = null
        _state.update { it.copy(testRunning = false, testOutput = null, testTiming = null, testError = null) }
    }

    fun runTest() {
        if (_state.value.testRunning) return
        _state.update { it.copy(testRunning = true, testOutput = null, testTiming = null, testError = null) }
        // LAZY so testJob is assigned before the body runs; otherwise a fast engine can reach the
        // finally check below first and leave testRunning stuck at true.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
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
                if (testJob == coroutineContext[Job]) _state.update { it.copy(testRunning = false) }
            }
        }
        testJob = job
        job.start()
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
