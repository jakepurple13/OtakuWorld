package com.programmersbox.mangaworld

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.components.BackButton
import com.programmersbox.mihon.MihonApiService
import com.programmersbox.mihon.preferences.MihonPreferenceFragment
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MihonPreferencesScreen(serviceName: String) {
    val sourceRepository = koinInject<SourceRepository>()
    val title = (sourceRepository.toSourceByApiServiceName(serviceName)?.apiService as? MihonApiService)
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
        AndroidFragment<MihonPreferenceFragment>(
            arguments = bundleOf(MihonPreferenceFragment.ARG_SERVICE_NAME to serviceName),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        )
    }
}
