package com.programmersbox.kmpuiviews.presentation.details

import com.programmersbox.kmpuiviews.domain.aitext.AiTextState

/** A finished TL;DR and whether it was summarized from a translated description. */
data class TldrEntry(val done: AiTextState.Done, val translated: Boolean)

/** Finished TL;DRs keyed by item URL, kept in memory for the app session. */
class TldrCache {
    private val entries = mutableMapOf<String, TldrEntry>()

    operator fun get(url: String): TldrEntry? = synchronized(entries) { entries[url] }

    operator fun set(url: String, entry: TldrEntry) {
        synchronized(entries) { entries[url] = entry }
    }
}
