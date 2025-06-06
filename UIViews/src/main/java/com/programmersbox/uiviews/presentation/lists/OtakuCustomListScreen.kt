package com.programmersbox.uiviews.presentation.lists

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.viewmodel.compose.viewModel
import com.programmersbox.datastore.DataStoreHandler
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.CustomListItem
import com.programmersbox.favoritesdatabase.ListDao
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuCustomListScreen
import com.programmersbox.kmpuiviews.presentation.settings.lists.OtakuCustomListViewModel
import com.programmersbox.kmpuiviews.utils.LocalCustomListDao
import com.programmersbox.uiviews.utils.LightAndDarkPreviews
import com.programmersbox.uiviews.utils.PreviewTheme
import java.util.UUID

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