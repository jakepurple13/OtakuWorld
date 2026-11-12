package com.programmersbox.mihon

import android.content.Context

/**
 * Keeps repo signing keys (repo base URL -> fingerprint) across restarts, so repo-signed extensions stay
 * trusted when the repos cannot be fetched at startup.
 */
internal class RepoSigningKeyStore(context: Context, name: String) {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    // Entries are "<base url> <fingerprint>"; fingerprints are hex, so the last space separates them.
    fun load(): Map<String, String> = prefs.getStringSet(KEY, null).orEmpty()
        .filter { ' ' in it }
        .associate { it.substringBeforeLast(' ') to it.substringAfterLast(' ') }

    fun save(keys: Map<String, String>) {
        if (keys == load()) return
        prefs.edit().putStringSet(KEY, keys.map { (base, key) -> "$base $key" }.toSet()).apply()
    }

    private companion object {
        const val KEY = "keysByRepo"
    }
}
