package com.programmersbox.kmpuiviews.translation.bergamot

import com.programmersbox.kmpuiviews.translation.EngineKind
import com.programmersbox.kmpuiviews.translation.TranslationEngine
import com.programmersbox.kmpuiviews.translation.TranslationUnavailable

/** Offline translation with Firefox's Bergamot models; pivots through English when there's no direct pair. */
class BergamotEngine(
    private val store: BergamotModelStore,
    private val runtime: BergamotRuntime,
) : TranslationEngine {
    override val kind = EngineKind.Bergamot

    override suspend fun targets(): List<String> = store.installedPairs().map { it.to }.distinct()

    override suspend fun translate(text: String, source: String, target: String): String {
        runtime.unavailableReason()?.let { throw TranslationUnavailable(it) }
        val steps = store.filesFor(source, target) ?: throw TranslationUnavailable(
            "Download ${bergamotLanguageName(source)} → ${bergamotLanguageName(target)} in Settings → Translation"
        )
        return steps.fold(text) { current, model -> runtime.translate(current, model) }
    }
}
