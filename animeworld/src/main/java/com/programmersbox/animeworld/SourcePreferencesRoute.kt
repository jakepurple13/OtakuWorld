package com.programmersbox.animeworld

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.programmersbox.cloudstream.preferences.CloudstreamSettingsActivity
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import org.koin.compose.koinInject

/** Dispatches a source's preferences screen by its service-name prefix; add new source families here. */
@Composable
fun SourcePreferencesRoute(serviceName: String) {
    when {
        serviceName.startsWith("aniyomi:") -> AniyomiPreferencesScreen(serviceName)
        serviceName.startsWith("cloudstream:") -> CloudstreamPreferencesRoute(serviceName)
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

/** CloudStream plugins show their own AppCompat UI, so it runs in its own activity and this entry pops. */
@Composable
private fun CloudstreamPreferencesRoute(serviceName: String) {
    val context = LocalContext.current
    val navigationActions = koinInject<NavigationActions>()
    LaunchedEffect(serviceName) {
        context.startActivity(CloudstreamSettingsActivity.intent(context, serviceName))
        navigationActions.popBackStack()
    }
}
