package com.programmersbox.uiviews.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.all.AllScreen
import com.programmersbox.kmpuiviews.presentation.favorite.FavoriteScreen
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchScreen
import com.programmersbox.kmpuiviews.presentation.globalsearch.GlobalSearchViewModel
import com.programmersbox.kmpuiviews.presentation.globalsearch.SearchCoverCard
import com.programmersbox.kmpuiviews.presentation.recent.RecentView
import com.programmersbox.kmpuiviews.presentation.settings.general.GeneralSettings
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuCustomListScreen
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuCustomListViewModel
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuListView
import com.programmersbox.kmpuiviews.utils.LocalCustomListDao
import com.programmersbox.kmpuiviews.utils.LocalHistoryDao
import java.util.UUID

@LightAndDarkPreviews
@Composable
private fun AllScreenPreview() {
    PreviewTheme {
        AllScreen(
            itemInfoChange = {},
            state = rememberLazyGridState(),
            isRefreshing = true,
            sourceList = emptyList(),
            favoriteList = emptyList(),
            onLoadMore = {},
            onReset = {},
            paddingValues = PaddingValues(0.dp)
        )
    }
}

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
@LightAndDarkPreviews
@Composable
private fun FavoriteScreenPreview() {
    PreviewTheme {
        FavoriteScreen()
    }
}

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

@LightAndDarkPreviews
@Composable
private fun CustomListScreenPreview() {
    PreviewTheme {
        val listDao: ListDao = LocalCustomListDao.current
        val context = LocalContext.current
        val viewModel: OtakuCustomListViewModel = viewModel {
            OtakuCustomListViewModel(
                screen = Screen.CustomListScreen.CustomListItem(""),
                listDao,
                DataStoreHandler(
                    defaultValue = false,
                    key = booleanPreferencesKey("asdf")
                )
            )
        }
        OtakuCustomListScreen(
            viewModel = viewModel,
            customItem = CustomList(
                item = CustomListItem(
                    uuid = UUID.randomUUID().toString(),
                    name = "Hello",
                ),
                list = emptyList()
            ),
            writeToFile = viewModel::writeToFile,
            navigateBack = {},
            isHorizontal = false,
            deleteAll = viewModel::deleteAll,
            rename = viewModel::rename,
            searchQuery = viewModel.searchQuery,
            setQuery = viewModel::setQuery,
            addSecurityItem = {},
            removeSecurityItem = {},
        )
    }
}

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

@PreviewThemeColorsSizes
@Composable
private fun RecentPreview() {
    PreviewTheme {
        RecentView()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@LightAndDarkPreviews
@Composable
private fun GeneralSettingsPreview() {
    PreviewTheme {
        GeneralSettings()
    }
}