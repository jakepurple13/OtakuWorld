package com.programmersbox.uiviews.presentation.globalsearch

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchScreen
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchViewModel
import com.programmersbox.kmpuiviews.presentation.globalsearch.SearchCoverCard
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.MockApiService
import com.programmersbox.uiviews.utils.PreviewTheme

@LightAndDarkPreviews
@Composable
private fun GlobalScreenPreview() {
    PreviewTheme {
        val dao = LocalHistoryDao.current
        GlobalSearchScreen(
            dao = dao,
            isHorizontal = false,
            screen = Screen.GlobalSearchScreen(),
            viewModel = viewModel {
                GlobalSearchViewModel(
                    sourceRepository = SourceRepository(),
                    handle = Screen.GlobalSearchScreen(),
                    dao = dao,
                )
            }
        )
    }
}

@LightAndDarkPreviews
@Composable
private fun GlobalSearchCoverPreview() {
    PreviewTheme {
        SearchCoverCard(
            model = KmpItemModel(
                title = "Title",
                description = "Description",
                url = "url",
                imageUrl = "imageUrl",
                source = MockApiService
            ),
            onLongPress = {}
        )
    }
}