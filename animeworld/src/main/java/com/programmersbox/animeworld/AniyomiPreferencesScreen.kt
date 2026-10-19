package com.programmersbox.animeworld

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import com.programmersbox.aniyomi.AniyomiApiService
import com.programmersbox.aniyomi.preferences.AniyomiPreferenceFragment
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.sharedcomponents.components.HideNavBarWhileOnScreen
import org.koin.compose.koinInject

@Composable
fun AniyomiPreferencesScreen(serviceName: String) {
    HideNavBarWhileOnScreen()
    val sourceRepository = koinInject<SourceRepository>()
    val title = (sourceRepository.toSourceByApiServiceName(serviceName)?.apiService as? AniyomiApiService)
        ?.displayName
        ?: serviceName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { BackButton() },
            )
        }
    ) { padding ->
        AndroidFragment<AniyomiPreferenceFragment>(
            arguments = bundleOf(AniyomiPreferenceFragment.ARG_SERVICE_NAME to serviceName),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        )
    }
}
