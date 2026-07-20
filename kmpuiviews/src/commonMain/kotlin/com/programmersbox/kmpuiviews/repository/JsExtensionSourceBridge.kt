package com.programmersbox.kmpuiviews.repository

import com.programmersbox.jsextensionloader.JsExtension
import com.programmersbox.jsextensionloader.JsExtensionRepository
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Reactively mirrors [JsExtensionRepository] into [SourceRepository] so JS
 * extensions appear alongside real sources with no other wiring required.
 * Diffs by extension id AND instance identity — an auto-update reload
 * replaces a same-id extension in place, and the mirrored entry must swap to
 * the new instance rather than keep pointing at a closed [JsExtension].
 *
 * Also self-heals: the legacy JAR/APK loader calls `SourceRepository.setSources(...)`
 * (a full replace, not a merge) every time its own extension-directory watcher
 * fires, independently of anything this bridge does — which silently wipes
 * out any JS-mirrored entries. Since that legacy loader can't be modified,
 * this bridge also observes [SourceRepository.sources] itself and re-adds any
 * of its own entries that go missing.
 */
class JsExtensionSourceBridge(
    private val jsExtensionRepository: JsExtensionRepository,
    private val sourceRepository: SourceRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mirrored = mutableMapOf<String, Pair<JsExtension, KmpSourceInformation>>()

    init {
        combine(jsExtensionRepository.extensions, sourceRepository.sources) { extensions, sources -> extensions to sources }
            .onEach { (extensions, sources) -> sync(extensions, sources) }
            .launchIn(scope)
    }

    private fun sync(current: List<JsExtension>, sourcesNow: List<KmpSourceInformation>) {
        val currentById = current.associateBy { it.manifest.id }

        (mirrored.keys - currentById.keys).forEach { id ->
            mirrored.remove(id)?.let { (_, info) ->
                runCatching { sourceRepository.removeSource(info) }
            }
        }

        currentById.forEach { (id, extension) ->
            val existing = mirrored[id]
            val stillPresent = existing != null && existing.second in sourcesNow
            if (existing == null || existing.first !== extension || !stillPresent) {
                runCatching {
                    if (existing != null && stillPresent && existing.first !== extension) {
                        sourceRepository.removeSource(existing.second)
                    }
                    val info = KmpSourceInformation(
                        apiService = JsApiServiceAdapter(extension),
                        name = extension.manifest.name,
                        icon = null,
                        packageName = "js.${extension.manifest.id}",
                    )
                    sourceRepository.addSource(info)
                    mirrored[id] = extension to info
                }
            }
        }
    }
}
