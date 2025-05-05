package com.programmersbox.uiviews.presentation.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.presentation.settings.moreinfo.MoreInfoScreen
import com.programmersbox.uiviews.BuildConfig
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoSettings(
    usedLibraryClick: () -> Unit,
    onPrereleaseClick: () -> Unit,
) {
    MoreInfoScreen(
        usedLibraryClick = usedLibraryClick,
        onPrereleaseClick = onPrereleaseClick,
        shouldShowPrerelease = BuildConfig.DEBUG
    )
}

@LightAndDarkPreviews
@Composable
private fun InfoSettingsPreview() {
    PreviewTheme {
        InfoSettings(
            usedLibraryClick = {},
            onPrereleaseClick = {}
        )
    }
}