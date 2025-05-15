package com.programmersbox.uiviews.presentation.lists

import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuListView
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme

@LightAndDarkPreviews
@Composable
private fun ListScreenPreview() {
    PreviewTheme {
        OtakuListView(
            customLists = emptyList(),
            customItem = null,
            navigateDetail = {}
        )
    }
}