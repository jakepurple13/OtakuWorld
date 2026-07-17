package com.programmersbox.kmpuiviews.repository

import com.programmersbox.jsextensionloader.JsExtension
import com.programmersbox.jsextensionloader.JsExtensionRepository
import com.programmersbox.kmpmodels.KmpSourceInformation
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Reactively mirrors [JsExtensionRepository] into [SourceRepository] so JS
 * extensions appear alongside real sources with no other wiring required.
 * Diffs by extension id AND instance identity — an auto-update reload
 * replaces a same-id extension in place, and the mirrored entry must swap to
 * the new instance rather than keep pointing at a closed [JsExtension].
 */
class JsExtensionSourceBridge(
    private val jsExtensionRepository: JsExtensionRepository,
    private val sourceRepository: SourceRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val mirrored = mutableMapOf<String, Pair<JsExtension, KmpSourceInformation>>()

    init {
        jsExtensionRepository.extensions
            .onEach { current -> sync(current) }
            .launchIn(scope)
    }

    private fun sync(current: List<JsExtension>) {
        val currentById = current.associateBy { it.manifest.id }

        (mirrored.keys - currentById.keys).forEach { id ->
            mirrored.remove(id)?.let { (_, info) ->
                runCatching { sourceRepository.removeSource(info) }
            }
        }

        currentById.forEach { (id, extension) ->
            val existing = mirrored[id]
            if (existing == null || existing.first !== extension) {
                runCatching {
                    existing?.let { (_, oldInfo) -> sourceRepository.removeSource(oldInfo) }
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
