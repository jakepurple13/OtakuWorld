package com.programmersbox.kmpuiviews.presentation.debug.navmap

/** Reads the generated `navgraph.json`; null when this build doesn't ship one (release builds, desktop, iOS). */
fun interface NavGraphSource {
    suspend fun read(): String?
}
