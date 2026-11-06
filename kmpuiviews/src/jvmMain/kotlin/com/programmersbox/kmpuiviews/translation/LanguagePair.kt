package com.programmersbox.kmpuiviews.translation

/** One translation direction a local model covers, e.g. `ja` → `en`. */
interface LanguagePair {
    val from: String
    val to: String
}

/**
 * Returns the pair chain for [source] → [target]: direct if present, else `[source→en, en→target]`;
 * null if impossible. Empty when source == target (the caller skips translation).
 */
fun <P : LanguagePair> planChain(source: String, target: String, pairs: List<P>): List<P>? {
    if (source == target) return emptyList()
    pairs.firstOrNull { it.from == source && it.to == target }?.let { return listOf(it) }
    if (source == PIVOT || target == PIVOT) return null
    val first = pairs.firstOrNull { it.from == source && it.to == PIVOT } ?: return null
    val second = pairs.firstOrNull { it.from == PIVOT && it.to == target } ?: return null
    return listOf(first, second)
}

private const val PIVOT = "en"
