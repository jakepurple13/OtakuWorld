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
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.TemplateEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SummaryState {
    /** Shimmer. */
    data object Loading : SummaryState
    data class Streaming(val text: String, val label: String, val onDevice: Boolean) : SummaryState
    data class Done(
        val text: String,
        val label: String,
        val onDevice: Boolean,
        val engine: SummaryEngineId,
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
            val facts = factsBuilder.yesterday()
            val choice = settings.prefs.first().engine
            val engine = resolver.resolve(choice)
            _uiState.update {
                it.copy(
                    facts = facts,
                    needsSetup = engine.id == SummaryEngineId.Template && choice != SummaryEngineChoice.Template,
                )
            }

            if (!bypassCache) {
                // A cache for any other date is stale and never shown.
                settings.cached()?.takeIf { it.date == facts.date }?.let { cached ->
                    publishDone(cached.text, resolver.engine(cached.engine))
                    return@launch
                }
            }

            if (facts.isQuiet || engine.id == SummaryEngineId.Template) {
                publishTemplate(facts)
                return@launch
            }

            val label = engine.label()
            try {
                var text = ""
                engine.summarize(facts)
                    .scan("") { acc, chunk -> acc + chunk }
                    .collect { accumulated ->
                        text = accumulated
                        _uiState.update {
                            it.copy(summary = SummaryState.Streaming(accumulated, label, engine.onDevice))
                        }
                    }
                if (text.isBlank()) {
                    publishTemplate(facts)
                } else {
                    settings.cache(DailySummary(facts.date, engine.id, text))
                    publishDone(text, engine)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                publishTemplate(facts)
            }
        }
    }

    private suspend fun publishTemplate(facts: DailyFacts) =
        publishDone(template.summarize(facts).first(), template)

    private suspend fun publishDone(text: String, engine: SummaryEngine) {
        val label = engine.label()
        _uiState.update {
            it.copy(summary = SummaryState.Done(text, label, engine.onDevice, engine.id))
        }
    }
}
