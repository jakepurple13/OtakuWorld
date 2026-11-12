package com.programmersbox.kmpuiviews.dailyoverview

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.programmersbox.kmpuiviews.domain.dailyoverview.DailyFacts
import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.NanoFeatureStatus
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryPrompt
import com.programmersbox.kmpuiviews.domain.dailyoverview.toEngineStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import otakuworld.kmpuiviews.generated.resources.Res
import otakuworld.kmpuiviews.generated.resources.do_label_nano
import otakuworld.kmpuiviews.generated.resources.do_setup_nano_downloading
import java.util.concurrent.atomic.AtomicBoolean

/** Gemini Nano through the ML Kit GenAI Prompt API. First choice on supported phones. */
class NanoEngine : SummaryEngine {
    override val id = SummaryEngineId.Nano
    override val onDevice = true

    private val model: GenerativeModel by lazy { Generation.getClient() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadStarted = AtomicBoolean(false)

    override suspend fun label(): String = getString(Res.string.do_label_nano)

    override suspend fun status(): EngineStatus {
        val status = runCatching { model.checkStatus().toNanoStatus() }
            .getOrDefault(NanoFeatureStatus.Unavailable)
        if (status == NanoFeatureStatus.Downloadable && downloadStarted.compareAndSet(false, true)) {
            scope.launch {
                runCatching { model.download().collect() }
                    .onFailure { downloadStarted.set(false) }
            }
        }
        return status.toEngineStatus(getString(Res.string.do_setup_nano_downloading))
    }

    override fun summarize(facts: DailyFacts): Flow<String> =
        generate(SummaryPrompt.system(facts.unit), SummaryPrompt.user(facts))

    override fun generate(system: String, user: String): Flow<String> {
        // Stream chunks are already deltas.
        return model.generateContentStream(system + "\n\n" + user).map { it.candidates.firstOrNull()?.text.orEmpty() }
    }

    private fun Int.toNanoStatus() = when (this) {
        FeatureStatus.AVAILABLE -> NanoFeatureStatus.Available
        FeatureStatus.DOWNLOADABLE -> NanoFeatureStatus.Downloadable
        FeatureStatus.DOWNLOADING -> NanoFeatureStatus.Downloading
        else -> NanoFeatureStatus.Unavailable
    }
}
