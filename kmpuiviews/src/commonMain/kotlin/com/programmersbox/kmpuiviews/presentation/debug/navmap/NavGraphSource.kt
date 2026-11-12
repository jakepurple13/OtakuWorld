package com.programmersbox.kmpuiviews.presentation.debug.navmap

/** Reads the generated `navgraph.json`; null when this build doesn't ship one (release builds, packaged desktop, iOS). */
fun interface NavGraphSource {
    suspend fun read(): String?

    /** Whether this build has a graph at all; the Developer screen only shows the Navigation map row when true. */
    val available: Boolean get() = true
}
