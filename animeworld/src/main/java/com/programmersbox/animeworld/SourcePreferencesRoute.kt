package com.programmersbox.animeworld

import androidx.compose.runtime.Composable

/** Dispatches a source's preferences screen by its service-name prefix; add new source families here. */
@Composable
fun SourcePreferencesRoute(serviceName: String) {
    when {
        serviceName.startsWith("aniyomi:") -> AniyomiPreferencesScreen(serviceName)
    }
}
