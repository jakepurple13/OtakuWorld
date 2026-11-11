package com.programmersbox.kmpuiviews.presentation.dailyoverview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFactsBuilder
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyOverviewSettings
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailySummary
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineResolver
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineChoice
import com.programmersbox.kmpuiviews.domain.dailyoverview.cacheOrIgnore
import com.programmersbox.kmpuiviews.domain.dailyoverview.cachedFor
import com.programmersbox.kmpuiviews.domain.dailyoverview.needsEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.TemplateEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SummaryState {
    /** Shimmer. */
    data object Loading : SummaryState
    /** [destination] names who receives the facts when not [onDevice], e.g. "Google". */
    data class Streaming(
        val text: String,
        val label: String,
        val onDevice: Boolean,
        val destination: String? = null,
    ) : SummaryState
    data class Done(
        val text: String,
        val label: String,
        val onDevice: Boolean,
        val engine: SummaryEngineId,
        val destination: String? = null,
    ) : SummaryState
}

data class DailyOverviewUiState(
    val facts: DailyFacts?,
    val summary: SummaryState,
    val needsSetup: Boolean,
)

class DailyOverviewViewModel(
    private val factsBuilder: DailyFactsBuilder,
    private val resolver: EngineResolver,
    private val settings: DailyOverviewSettings,
    private val template: TemplateEngine,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DailyOverviewUiState(facts = null, summary = SummaryState.Loading, needsSetup = false)
    )
    val uiState: StateFlow<DailyOverviewUiState> = _uiState.asStateFlow()

    private var job: Job? = null

    init {
        load(bypassCache = false)
    }

    fun regenerate() = load(bypassCache = true)

    private fun load(bypassCache: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(summary = SummaryState.Loading) }
            // Stats show right away; the engine is only resolved when the summary needs it.
            val facts = factsBuilder.yesterday()
            _uiState.update { it.copy(facts = facts) }

            if (!bypassCache) {
                // A cache for any other date is stale and never shown.
                settings.cachedFor(facts)?.let { cached ->
                    publishDone(cached.text, resolver.engine(cached.engine))
                    return@launch
                }
            }

            if (facts.isQuiet) {
                publishTemplate(facts)
                // Still offer the setup chip, checked after the template text is up.
                resolveAndFlagSetup()
                return@launch
            }

            val engine = resolveAndFlagSetup()
            if (!facts.needsEngine(engine)) {
                publishTemplate(facts)
                return@launch
            }

            val label = engine.label()
            val destination = engine.destination()
            var text = ""
            try {
                engine.summarize(facts)
                    .scan("") { acc, chunk -> acc + chunk }
                    // The shimmer stays until there is text to show.
                    .filter { it.isNotBlank() }
                    .collect { accumulated ->
                        text = accumulated
                        _uiState.update {
                            it.copy(summary = SummaryState.Streaming(accumulated, label, engine.onDevice, destination))
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                publishTemplate(facts)
                return@launch
            }
            if (text.isBlank()) {
                publishTemplate(facts)
            } else {
                settings.cacheOrIgnore(DailySummary(facts.date, engine.id, text))
                publishDone(text, engine)
            }
        }
    }

    /** The chosen engine, flagging setup when a non-Template choice fell back to Template. */
    private suspend fun resolveAndFlagSetup(): SummaryEngine {
        val choice = settings.prefs.first().engine
        val engine = resolver.resolve(choice)
        _uiState.update {
            it.copy(needsSetup = engine.id == SummaryEngineId.Template && choice != SummaryEngineChoice.Template)
        }
        return engine
    }

    private suspend fun publishTemplate(facts: DailyFacts) =
        publishDone(template.summarize(facts).first(), template)

    private suspend fun publishDone(text: String, engine: SummaryEngine) {
        val label = engine.label()
        val destination = engine.destination()
        _uiState.update {
            it.copy(summary = SummaryState.Done(text, label, engine.onDevice, engine.id, destination))
        }
    }
}
