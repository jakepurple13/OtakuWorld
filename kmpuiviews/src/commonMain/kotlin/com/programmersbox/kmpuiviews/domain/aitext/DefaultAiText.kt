package com.programmersbox.kmpuiviews.domain.aitext

import com.programmersbox.kmpuiviews.domain.dailyoverview.EngineResolver
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngine
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineChoice
import com.programmersbox.kmpuiviews.domain.dailyoverview.SummaryEngineId
import com.programmersbox.kmpuiviews.utils.dispatchIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class DefaultAiText(
    engines: List<SummaryEngine>,
    private val nanoTools: NanoTools?,
    order: List<SummaryEngineId> = onDeviceAiOrder,
) : AiText {
    private val resolver = EngineResolver(engines, order)
    private val withoutNano = EngineResolver(engines, order - SummaryEngineId.Nano)

    override suspend fun engineFor(feature: AiFeature, inputLength: Int): AiEngineInfo? =
        withContext(Dispatchers.IO) {
            resolve(feature, inputLength)?.let { engine ->
                AiEngineInfo(engine.label(), engine.onDevice, engine.destination())
            }
        }

    override fun recap(previousChapterText: String): Flow<String> =
        run(AiFeature.Recap, AiTextPrompts.recapTail(previousChapterText))

    override fun tldr(description: String): Flow<String> = run(AiFeature.Tldr, description)

    override fun proofread(text: String): Flow<String> = run(AiFeature.Proofread, text)

    override fun rewrite(text: String, style: RewriteStyle): Flow<String> = run(AiFeature.Rewrite, text, style)

    override fun listNames(titles: List<String>): Flow<String> =
        run(AiFeature.ListSuggest, titles.take(AiTextPrompts.MAX_LIST_TITLES).joinToString("\n"))

    // Nano tools have no list-description task, so this always goes through the engine's prompt API.
    override fun listDescription(titles: List<String>): Flow<String> {
        val input = titles.take(AiTextPrompts.MAX_LIST_TITLES).joinToString("\n")
        return flow {
            val engine = resolve(AiFeature.ListSuggest, input.length) ?: throw noEngine()
            emitAll(engine.generate(AiTextPrompts.LIST_DESCRIPTION_SYSTEM, AiTextPrompts.user(AiFeature.ListSuggest, input)))
        }.dispatchIo()
    }

    /** Null when nothing on-device is ready. Template means the resolver found nothing. */
    private suspend fun resolve(feature: AiFeature, inputLength: Int): SummaryEngine? {
        val tooLongForNano = (feature == AiFeature.Proofread || feature == AiFeature.Rewrite) &&
                inputLength > AiTextPrompts.NANO_TEXT_LIMIT
        val r = if (tooLongForNano) withoutNano else resolver
        return r.resolve(SummaryEngineChoice.Automatic).takeIf { it.id != SummaryEngineId.Template }
    }

    private fun run(feature: AiFeature, input: String, style: RewriteStyle? = null): Flow<String> = flow {
        val engine = resolve(feature, input.length) ?: throw noEngine()
        val tools = nanoTools
        if (engine.id == SummaryEngineId.Nano && tools != null && tools.isReady(feature, input.length)) {
            emitAll(tools.run(feature, input, style))
        } else {
            emitAll(engine.generate(AiTextPrompts.system(feature, style), AiTextPrompts.user(feature, input)))
        }
    }.dispatchIo()

    private fun noEngine() = IllegalStateException("No on-device AI engine is ready")
}
