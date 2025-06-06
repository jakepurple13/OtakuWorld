package com.programmersbox.uiviews.presentation.favorite

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteScreen
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@LightAndDarkPreviews
@Composable
private fun FavoriteScreenPreview() {
    PreviewTheme {
        FavoriteScreen()
    }
}