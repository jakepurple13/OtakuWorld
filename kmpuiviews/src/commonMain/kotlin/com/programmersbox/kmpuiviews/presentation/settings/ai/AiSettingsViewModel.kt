package com.programmersbox.kmpuiviews.presentation.settings.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.kmpuiviews.domain.aitext.AiEnginePreference
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiSettings
import com.programmersbox.kmpuiviews.domain.aitext.AiTask
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.domain.aitext.AiToggle
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.platformAutomaticOrder
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ImportState
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ImportedModel
import com.programmersbox.kmpuiviews.domain.aitext.modelimport.ModelImports
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiEngineStatus(val id: SummaryEngineId, val name: String, val status: EngineStatus)

data class AiSettingsUi(
    val enabled: Boolean,
    val preference: AiEnginePreference,
    /** False where only one on-device engine exists (desktop has just Ollama). */
    val showPreference: Boolean,
    /** The engine AI features would use right now, or null when none is ready. */
    val current: String?,
    val engines: List<AiEngineStatus>,
    val disabled: Set<AiToggle>,
    /** True where model files can be imported (Android, desktop). */
    val canImport: Boolean = false,
    /** The model file the local engine runs, or null. */
    val localModel: ImportedModel? = null,
)

/** (features on, total), or null when the master switch is off. Shown on the Integrations row. */
fun AiSettingsUi.summary(): Pair<Int, Int>? =
    if (!enabled) null else (AiToggle.entries.size - disabled.size) to AiToggle.entries.size

/** A made-up description for "Try it", so a test run never reads the user's library. */
internal const val SAMPLE_DESCRIPTION =
    "Maomao, a young woman trained in medicine by her apothecary father, is kidnapped and sold as a servant " +
            "to the emperor's palace. She keeps her skills hidden until the emperor's infant children fall ill. " +
            "When she secretly warns the concubines that their makeup is poisoning them, the eunuch Jinshi notices, " +
            "promotes her to food taster, and starts bringing her every strange case in the rear palace. Each " +
            "mystery pulls her deeper into court intrigue she would rather avoid."

class AiSettingsViewModel(
    private val settings: AiSettings,
    private val aiText: AiText,
    private val engines: List<SummaryEngine>,
    private val order: List<SummaryEngineId> = platformAutomaticOrder,
    private val imports: ModelImports? = null,
) : ViewModel() {
    private class EngineInfo(val current: String?, val statuses: List<AiEngineStatus>, val localModel: ImportedModel?)

    val importState: StateFlow<ImportState> = imports?.state ?: MutableStateFlow(ImportState.Idle)

    private val engineInfo = MutableStateFlow<EngineInfo?>(null)
    private val refreshes = MutableStateFlow(0)

    // On-device engines this platform has, in the platform's order
    private val onDevice = order
        .filter { it != SummaryEngineId.Provider && it != SummaryEngineId.Template }
        .mapNotNull { id -> engines.firstOrNull { it.id == id } }

    /** Null until settings and engine statuses have loaded. */
    val ui: StateFlow<AiSettingsUi?> = combine(
        settings.enabled,
        settings.preference,
        settings.disabled,
        engineInfo,
    ) { enabled, preference, disabled, info ->
        info ?: return@combine null
        AiSettingsUi(
            enabled = enabled,
            preference = preference,
            // The choices name Nano, so only where Nano is one of the engines
            showPreference = onDevice.size > 1 && onDevice.any { it.id == SummaryEngineId.Nano },
            current = info.current,
            engines = info.statuses,
            disabled = disabled,
            canImport = imports != null,
            localModel = info.localModel,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val sampleTask = AiTask(aiText, viewModelScope)
    val sample: StateFlow<AiTextState> = sampleTask.state

    init {
        // Statuses can be slow (AICore, Ollama), so only settings that change the answer and an
        // explicit refresh recompute them.
        viewModelScope.launch {
            combine(settings.enabled, settings.preference, refreshes) { _, _, _ -> }
                .collectLatest { engineInfo.value = loadEngines() }
        }
        // A finished or failed import changes which model the local engine has
        viewModelScope.launch {
            var busy = false
            importState.collect { state ->
                val nowBusy = state is ImportState.Copying || state is ImportState.Checking
                if (busy && !nowBusy) refresh()
                busy = nowBusy
            }
        }
    }

    /** Imports [file]; with [replace], the current model is deleted once the new one passes its check. */
    fun importModel(file: PlatformFile, replace: Boolean) {
        imports?.import(file, replacing = if (replace) engineInfo.value?.localModel?.fileName else null)
    }

    fun cancelImport() {
        imports?.cancel()
    }

    fun dismissImportError() {
        imports?.dismissError()
    }

    fun removeModel() {
        val model = engineInfo.value?.localModel ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) { imports?.remove(model.fileName) }
            refresh()
        }
    }

    /** Re-reads engine statuses, e.g. after coming back from setting up Gemma. */
    fun refresh() = refreshes.update { it + 1 }

    fun setEnabled(v: Boolean) = save { settings.setEnabled(v) }
    fun setPreference(p: AiEnginePreference) = save { settings.setPreference(p) }
    fun setFeature(t: AiToggle, on: Boolean) = save { settings.setFeatureEnabled(t, on) }

    /** Streams a TL;DR of a fixed sample with the current engine. Ignores feature switches, not the master switch. */
    fun trySample() {
        viewModelScope.launch {
            if (!sampleTask.start(AiFeature.TrySample, SAMPLE_DESCRIPTION)) {
                sampleTask.fail("No on-device AI engine is ready")
            }
        }
    }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private suspend fun loadEngines(): EngineInfo = EngineInfo(
        current = orNull { aiText.engineFor(AiFeature.TrySample)?.label },
        statuses = onDevice.map { engine ->
            AiEngineStatus(
                id = engine.id,
                name = orNull { engine.shortName() } ?: engine.id.name,
                status = orNull { engine.status() } ?: EngineStatus.Unavailable,
            )
        },
        localModel = orNull { imports?.current() },
    )

    /** A failing engine call must not take the page down. */
    private suspend fun <T> orNull(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}
