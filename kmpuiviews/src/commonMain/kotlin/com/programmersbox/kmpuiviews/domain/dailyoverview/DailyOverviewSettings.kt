package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.domain.aitext.AiEnginePreference
import com.programmersbox.kmpuiviews.domain.aitext.AiSettings
import com.programmersbox.kmpuiviews.domain.aitext.AiToggle
import com.programmersbox.kmpuiviews.domain.aitext.AlwaysOnAiSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class SummaryEngineChoice { Automatic, Nano, Gemma, Provider, Ollama, Template }

data class DailyOverviewPrefs(
    val enabled: Boolean,
    val notify: Boolean,
    val minuteOfDay: Int,
    val skipQuiet: Boolean,
    /** The engine the user picked on the Summary engine screen. */
    val engine: SummaryEngineChoice,
    /** What actually runs once the AI settings apply: Template when AI is off, the AI preference for Automatic. */
    val effectiveEngine: SummaryEngineChoice = engine,
)

/** [stored] after the AI settings page: off means numbers only, and Automatic follows the engine preference. */
internal fun effectiveEngine(
    stored: SummaryEngineChoice,
    aiEnabled: Boolean,
    disabled: Set<AiToggle>,
    preference: AiEnginePreference,
): SummaryEngineChoice = when {
    !aiEnabled || AiToggle.DailyOverview in disabled -> SummaryEngineChoice.Template
    stored != SummaryEngineChoice.Automatic -> stored
    preference == AiEnginePreference.Nano -> SummaryEngineChoice.Nano
    preference == AiEnginePreference.Gemma -> SummaryEngineChoice.Gemma
    else -> SummaryEngineChoice.Automatic
}

@Serializable
data class DailySummary(
    val date: LocalDate,
    val engine: SummaryEngineId,
    val text: String,
)

class DailyOverviewSettings(
    private val dataStoreHandling: DataStoreHandling,
    private val aiSettings: AiSettings = AlwaysOnAiSettings,
) {
    private val stored: Flow<DailyOverviewPrefs> = combine(
        dataStoreHandling.dailyOverviewEnabled.asFlow(),
        dataStoreHandling.dailyOverviewNotify.asFlow(),
        dataStoreHandling.dailyOverviewTime.asFlow(),
        dataStoreHandling.dailyOverviewSkipQuiet.asFlow(),
        dataStoreHandling.dailyOverviewEngine.asFlow(),
    ) { enabled, notify, minuteOfDay, skipQuiet, engine ->
        DailyOverviewPrefs(
            enabled = enabled,
            notify = notify,
            minuteOfDay = minuteOfDay,
            skipQuiet = skipQuiet,
            engine = SummaryEngineChoice.entries.firstOrNull { it.name == engine }
                ?: SummaryEngineChoice.Automatic,
        )
    }

    val prefs: Flow<DailyOverviewPrefs> = combine(
        stored,
        aiSettings.enabled,
        aiSettings.disabled,
        aiSettings.preference,
    ) { prefs, aiEnabled, disabled, preference ->
        prefs.copy(effectiveEngine = effectiveEngine(prefs.engine, aiEnabled, disabled, preference))
    }

    suspend fun setEnabled(v: Boolean) = dataStoreHandling.dailyOverviewEnabled.set(v)
    suspend fun setNotify(v: Boolean) = dataStoreHandling.dailyOverviewNotify.set(v)
    suspend fun setMinuteOfDay(v: Int) = dataStoreHandling.dailyOverviewTime.set(v)
    suspend fun setSkipQuiet(v: Boolean) = dataStoreHandling.dailyOverviewSkipQuiet.set(v)
    suspend fun setEngine(v: SummaryEngineChoice) = dataStoreHandling.dailyOverviewEngine.set(v.name)

    suspend fun cached(): DailySummary? = dataStoreHandling.dailyOverviewCache.get()
        .takeIf { it.isNotEmpty() }
        ?.let { runCatching { json.decodeFromString(DailySummary.serializer(), it) }.getOrNull() }

    suspend fun cache(s: DailySummary) =
        dataStoreHandling.dailyOverviewCache.set(json.encodeToString(DailySummary.serializer(), s))

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
