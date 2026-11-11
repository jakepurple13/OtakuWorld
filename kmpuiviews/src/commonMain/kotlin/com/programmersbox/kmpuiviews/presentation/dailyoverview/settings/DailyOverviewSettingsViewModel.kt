package com.programmersbox.kmpuiviews.presentation.dailyoverview.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewPrefs
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewSettings
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineResolver
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineChoice
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.TitleRead
import com.programmersbox.kmpuiviews.domain.dailyoverview.UpdateItem
import com.programmersbox.kmpuiviews.domain.updatecheck.UpdateUnit
import com.programmersbox.kmpuiviews.presentation.dailyoverview.SummaryState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_engine_automatic
import otakuworld.kmpuiviews.generated.resources.do_engine_gemma
import otakuworld.kmpuiviews.generated.resources.do_engine_not_ready
import otakuworld.kmpuiviews.generated.resources.do_engine_numbers
import otakuworld.kmpuiviews.generated.resources.do_engine_ollama
import otakuworld.kmpuiviews.generated.resources.do_engine_provider
import otakuworld.kmpuiviews.generated.resources.do_engine_resolved
import otakuworld.kmpuiviews.generated.resources.do_label_nano

data class EngineRow(
    val id: SummaryEngineId,
    val status: EngineStatus,
    val label: String,
    val onDevice: Boolean,
    /** Who receives the facts when not [onDevice], e.g. "Google". */
    val destination: String? = null,
    /** e.g. "Gemma 4 E2B" or "Google · gemini-2.5-flash". */
    val shortName: String = label,
)

/**
 * [resolvedLabel] is the current choice as the settings row shows it. [automaticLabel] is what
 * Automatic picks right now. Both are empty until the engine statuses load.
 */
data class SettingsUi(
    val prefs: DailyOverviewPrefs,
    val resolvedLabel: String,
    val engines: List<EngineRow>,
    val automaticLabel: String = "",
)

class DailyOverviewSettingsViewModel(
    private val settings: DailyOverviewSettings,
    private val resolver: EngineResolver,
    private val genericInfo: KmpGenericInfo,
) : ViewModel() {
    private class EngineInfo(val resolvedLabel: String, val automaticLabel: String, val rows: List<EngineRow>)

    private val engineInfo = MutableStateFlow<EngineInfo?>(null)
    private val refreshes = MutableStateFlow(0)

    /** Null until the prefs load, so the switches don't flash their defaults. */
    val ui: StateFlow<SettingsUi?> = combine(settings.prefs, engineInfo) { prefs, info ->
        SettingsUi(
            prefs = prefs,
            resolvedLabel = info?.resolvedLabel.orEmpty(),
            engines = info?.rows.orEmpty(),
            automaticLabel = info?.automaticLabel.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _test = MutableStateFlow<SummaryState?>(null)
    val test: StateFlow<SummaryState?> = _test.asStateFlow()

    private var testJob: Job? = null

    init {
        // Statuses can be slow (Ollama is a network call), so only the engine choice and an
        // explicit refresh recompute them, not every switch.
        viewModelScope.launch {
            combine(settings.prefs.map { it.engine }.distinctUntilChanged(), refreshes) { choice, _ -> choice }
                .collectLatest { engineInfo.value = loadEngines(it) }
        }
    }

    /** Re-reads engine statuses, e.g. after coming back from the AI Helper. */
    fun refresh() = refreshes.update { it + 1 }

    fun setEnabled(v: Boolean) = save { settings.setEnabled(v) }
    fun setNotify(v: Boolean) = save { settings.setNotify(v) }
    fun setTime(minuteOfDay: Int) = save { settings.setMinuteOfDay(minuteOfDay) }
    fun setSkipQuiet(v: Boolean) = save { settings.setSkipQuiet(v) }
    fun choose(c: SummaryEngineChoice) = save { settings.setEngine(c) }

    /** Streams a sample summary from the chosen engine. Nothing is cached. */
    fun runTest() {
        testJob?.cancel()
        testJob = viewModelScope.launch {
            _test.value = SummaryState.Loading
            val facts = SampleFacts.create(genericInfo.updateUnit)
            val engine = resolver.resolve(settings.prefs.first().engine)
            var label = ""
            var destination: String? = null
            var text = ""
            try {
                label = engine.label()
                destination = engine.destination()
                engine.summarize(facts)
                    .scan("") { acc, chunk -> acc + chunk }
                    // The shimmer stays until there is text to show.
                    .filter { it.isNotBlank() }
                    .collect {
                        text = it
                        _test.value = SummaryState.Streaming(it, label, engine.onDevice, destination)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                text = ""
            }
            _test.value = if (text.isBlank()) {
                val template = resolver.engine(SummaryEngineId.Template)
                SummaryState.Done(
                    text = template.summarize(facts).first(),
                    label = template.label(),
                    onDevice = true,
                    engine = SummaryEngineId.Template,
                )
            } else {
                SummaryState.Done(text, label, engine.onDevice, engine.id, destination)
            }
        }
    }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private suspend fun loadEngines(choice: SummaryEngineChoice): EngineInfo {
        val rows = resolver.available().entries
            .sortedBy { it.key.ordinal }
            .map { (id, status) ->
                val engine = resolver.engine(id)
                val fallback = choiceName(SummaryEngineChoice.valueOf(id.name))
                EngineRow(
                    id = id,
                    status = status,
                    label = orNull { engine.label() } ?: fallback,
                    onDevice = engine.onDevice,
                    destination = orNull { engine.destination() },
                    shortName = orNull { engine.shortName() } ?: fallback,
                )
            }
        val automatic = resolver.resolve(SummaryEngineChoice.Automatic)
        val automaticLabel = rows.firstOrNull { it.id == automatic.id }?.shortName
            ?: getString(Res.string.do_engine_numbers)
        val resolved = if (choice == SummaryEngineChoice.Automatic) automatic else resolver.resolve(choice)
        val name = if (choice == SummaryEngineChoice.Automatic) {
            getString(Res.string.do_engine_resolved, choiceName(choice), automaticLabel)
        } else {
            choiceName(choice)
        }
        // Automatic landing on Numbers only is a working pick, not a failed one.
        val notReady = resolved.id == SummaryEngineId.Template &&
            choice != SummaryEngineChoice.Template && choice != SummaryEngineChoice.Automatic
        return EngineInfo(
            resolvedLabel = if (notReady) getString(Res.string.do_engine_not_ready, name) else name,
            automaticLabel = automaticLabel,
            rows = rows,
        )
    }

    /** A failing engine call must not take the page down; the caller shows a fallback. */
    private suspend fun <T> orNull(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    private suspend fun choiceName(choice: SummaryEngineChoice): String = getString(
        when (choice) {
            SummaryEngineChoice.Automatic -> Res.string.do_engine_automatic
            SummaryEngineChoice.Nano -> Res.string.do_label_nano
            SummaryEngineChoice.Gemma -> Res.string.do_engine_gemma
            SummaryEngineChoice.Provider -> Res.string.do_engine_provider
            SummaryEngineChoice.Ollama -> Res.string.do_engine_ollama
            SummaryEngineChoice.Template -> Res.string.do_engine_numbers
        }
    )
}

/** Fixed made-up facts for the Test card, so a test run never reads or sends the user's history. */
object SampleFacts {
    fun create(unit: UpdateUnit) = DailyFacts(
        date = LocalDate(2026, 11, 10),
        unit = unit,
        readCount = 23,
        titles = listOf(
            TitleRead("Frieren", "sample://frieren", "", "Sample", "Chapter 128", "Chapter 140", 13, caughtUp = true),
            TitleRead("Dandadan", "sample://dandadan", "", "Sample", "Chapter 170", "Chapter 175", 6, caughtUp = false),
            TitleRead("Blue Lock", "sample://bluelock", "", "Sample", "Chapter 290", "Chapter 293", 4, caughtUp = false),
        ),
        readsByHour = List(21) { 0 } + listOf(7, 9, 7),
        updates = listOf(
            UpdateItem("One Piece", "sample://onepiece", null, "Sample", "Chapter 1130"),
            UpdateItem("Kagurabachi", "sample://kagurabachi", null, "Sample", "Chapter 95"),
        ),
        failedSources = emptyList(),
    )
}
