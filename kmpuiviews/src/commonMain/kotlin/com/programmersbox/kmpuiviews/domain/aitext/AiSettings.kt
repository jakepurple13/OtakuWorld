package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** The switches on the AI settings page, one per feature. Every one starts on. */
enum class AiToggle { Tldr, ChapterRecap, WritingTools, ListSuggestions, ErrorExplanations, UpdateSummary, DailyOverview }

/** Which on-device engine AI text should use. A preferred engine that isn't ready means no engine. */
enum class AiEnginePreference { Automatic, Nano, Gemma }

/** The engine order for this preference, limited to engines [platform] has. */
fun AiEnginePreference.order(platform: List<SummaryEngineId>): List<SummaryEngineId> = when (this) {
    AiEnginePreference.Automatic -> platform
    AiEnginePreference.Nano -> listOf(SummaryEngineId.Nano).filter { it in platform }
    AiEnginePreference.Gemma -> listOf(SummaryEngineId.Gemma).filter { it in platform }
}

interface AiSettings {
    /** The master switch. Off hides every AI feature, Daily overview's summary included. */
    val enabled: Flow<Boolean>
    val preference: Flow<AiEnginePreference>
    val disabled: Flow<Set<AiToggle>>

    suspend fun setEnabled(v: Boolean)
    suspend fun setPreference(p: AiEnginePreference)
    suspend fun setFeatureEnabled(t: AiToggle, on: Boolean)
}

/** Everything on, Automatic. For callers built without settings, e.g. older tests. */
object AlwaysOnAiSettings : AiSettings {
    override val enabled: Flow<Boolean> = flowOf(true)
    override val preference: Flow<AiEnginePreference> = flowOf(AiEnginePreference.Automatic)
    override val disabled: Flow<Set<AiToggle>> = flowOf(emptySet())
    override suspend fun setEnabled(v: Boolean) = Unit
    override suspend fun setPreference(p: AiEnginePreference) = Unit
    override suspend fun setFeatureEnabled(t: AiToggle, on: Boolean) = Unit
}

/**
 * The disabled toggles from storage. Chapter recap lives in the reader's own `novelChapterRecap`
 * key, so the reader switch and the AI page switch are one setting; any stored name for it is ignored.
 */
internal fun storedDisabledToggles(stored: Set<String>, chapterRecap: Boolean): Set<AiToggle> {
    val fromSet = stored.mapNotNullTo(mutableSetOf()) { name ->
        AiToggle.entries.firstOrNull { it.name == name }?.takeIf { it != AiToggle.ChapterRecap }
    }
    if (!chapterRecap) fromSet += AiToggle.ChapterRecap
    return fromSet
}

class DataStoreAiSettings(private val dataStore: DataStoreHandling) : AiSettings {
    // Every key shares one preferences file, so any write re-emits; only real changes should restart work
    override val enabled: Flow<Boolean> = dataStore.aiEnabled.asFlow().distinctUntilChanged()

    override val preference: Flow<AiEnginePreference> = dataStore.aiEnginePreference.asFlow()
        .map { stored -> AiEnginePreference.entries.firstOrNull { it.name == stored } ?: AiEnginePreference.Automatic }
        .distinctUntilChanged()

    override val disabled: Flow<Set<AiToggle>> = combine(
        dataStore.aiDisabledFeatures.asFlow(),
        dataStore.novelChapterRecap.asFlow(),
        ::storedDisabledToggles,
    ).distinctUntilChanged()

    override suspend fun setEnabled(v: Boolean) = dataStore.aiEnabled.set(v)

    override suspend fun setPreference(p: AiEnginePreference) = dataStore.aiEnginePreference.set(p.name)

    override suspend fun setFeatureEnabled(t: AiToggle, on: Boolean) {
        if (t == AiToggle.ChapterRecap) {
            dataStore.novelChapterRecap.set(on)
            return
        }
        val current = dataStore.aiDisabledFeatures.get()
        dataStore.aiDisabledFeatures.set(if (on) current - t.name else current + t.name)
    }
}
