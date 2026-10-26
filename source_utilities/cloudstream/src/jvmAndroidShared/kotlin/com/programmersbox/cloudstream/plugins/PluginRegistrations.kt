package com.programmersbox.cloudstream.plugins

import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.extractorApis
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Plugins register into CloudStream's global lists. A load is attributed by diffing those lists
 * around it, so loads (and removals) are serialized on one lock.
 */
internal object PluginRegistrations {
    private val lock = Any()

    /** Runs [load] and returns what it registered; if it throws, those registrations are removed. */
    fun capture(load: () -> Unit): Pair<List<MainAPI>, List<ExtractorApi>> = synchronized(lock) {
        val apisBefore = identitySetOf(APIHolder.allProviders.withLock { APIHolder.allProviders.toList() })
        val extractorsBefore = identitySetOf(extractorApis.withLock { extractorApis.toList() })
        fun added(): Pair<List<MainAPI>, List<ExtractorApi>> =
            APIHolder.allProviders.withLock { APIHolder.allProviders.filterNot { it in apisBefore } } to
                extractorApis.withLock { extractorApis.filterNot { it in extractorsBefore } }
        try {
            load()
        } catch (t: Throwable) {
            val (apis, extractors) = added()
            remove(apis, extractors)
            throw t
        }
        added()
    }

    fun remove(apis: List<MainAPI>, extractors: List<ExtractorApi>) = synchronized(lock) {
        val apiSet = identitySetOf(apis)
        val extractorSet = identitySetOf(extractors)
        APIHolder.allProviders.withLock { APIHolder.allProviders.removeAll { it in apiSet } }
        apis.forEach { APIHolder.removePluginMapping(it) }
        extractorApis.withLock { extractorApis.removeAll { it in extractorSet } }
    }

    private fun <T> identitySetOf(items: List<T>): Set<T> =
        Collections.newSetFromMap(IdentityHashMap<T, Boolean>()).apply { addAll(items) }
}
