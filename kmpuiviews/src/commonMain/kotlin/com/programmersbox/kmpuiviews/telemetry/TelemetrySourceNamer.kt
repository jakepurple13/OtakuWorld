package com.programmersbox.kmpuiviews.telemetry

import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.kmpmodels.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The source name telemetry may send. Incognito sources become [INCOGNITO] and JS extensions
 * (package `js.*`) become [CUSTOM]; everything else keeps its own name.
 */
class TelemetrySourceNamer(
    private val sourceRepository: SourceRepository,
    private val dao: ItemDao,
) {
    // Package names and display names of sources the user marked incognito
    private val incognito = MutableStateFlow(emptySet<String>())

    fun start(scope: CoroutineScope) {
        dao.getAllIncognitoSources()
            .onEach { rows -> incognito.value = rows.filter { it.isIncognito }.flatMap { listOf(it.source, it.name) }.toSet() }
            .launchIn(scope)
    }

    fun name(serviceName: String): String {
        val packageName = sourceRepository.toSourceByApiServiceName(serviceName)?.packageName
        return when {
            serviceName in incognito.value || packageName in incognito.value -> INCOGNITO
            packageName?.startsWith(JS_PREFIX) == true -> CUSTOM
            else -> serviceName
        }
    }

    fun packageName(packageName: String): String = when {
        packageName in incognito.value -> INCOGNITO
        packageName.startsWith(JS_PREFIX) -> CUSTOM
        else -> packageName
    }

    companion object {
        const val INCOGNITO = "incognito"
        const val CUSTOM = "custom"
        private const val JS_PREFIX = "js."
    }
}
