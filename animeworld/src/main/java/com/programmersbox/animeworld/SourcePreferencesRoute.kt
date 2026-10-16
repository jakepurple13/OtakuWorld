package com.programmersbox.animeworld

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.programmersbox.kmpuiviews.presentation.components.BackButton

/** Dispatches a source's preferences screen by its service-name prefix; add new source families here. */
@Composable
fun SourcePreferencesRoute(serviceName: String) {
    when {
        serviceName.startsWith("aniyomi:") -> AniyomiPreferencesScreen(serviceName)
        else -> Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(serviceName) },
                    navigationIcon = { BackButton() },
                )
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No settings for this source")
            }
        }
    }
}
