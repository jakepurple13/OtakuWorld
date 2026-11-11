package com.programmersbox.kmpuiviews.domain.dailyoverview

import com.programmersbox.datastore.DataStoreHandling
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
    val engine: SummaryEngineChoice,
)

@Serializable
data class DailySummary(
    val date: LocalDate,
    val engine: SummaryEngineId,
    val text: String,
)

class DailyOverviewSettings(
    private val dataStoreHandling: DataStoreHandling,
) {
    val prefs: Flow<DailyOverviewPrefs> = combine(
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
