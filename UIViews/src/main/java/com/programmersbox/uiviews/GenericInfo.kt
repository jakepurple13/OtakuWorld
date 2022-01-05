package com.programmersbox.uiviews

import android.content.Context
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.uiviews.utils.ComponentState

interface GenericInfo {

    val apkString: AppUpdate.AppUpdates.() -> String?
    val scrollBuffer: Int get() = 2

    fun chapterOnClick(model: ChapterModel, allChapters: List<ChapterModel>, infoModel: InfoModel, context: Context)
    fun sourceList(): List<ApiService>
    fun searchList(): List<ApiService> = sourceList()
    fun toSource(s: String): ApiService?
    fun customPreferences(preferenceScreen: SettingsDsl) = Unit
    fun composeCustomPreferences(navController: NavController): ComposeSettingsDsl.() -> Unit = {}
    fun downloadChapter(model: ChapterModel, allChapters: List<ChapterModel>, infoModel: InfoModel, context: Context)

    @Composable
    fun DetailActions(infoModel: InfoModel, tint: Color) {
    }

    @Composable
    fun ComposeShimmerItem()

    @Composable
    fun ItemListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyListState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        onClick: (ItemModel) -> Unit
    )

    @Composable
    fun AllListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyListState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        onClick: (ItemModel) -> Unit
    ) = ItemListView(list, favorites, listState, onLongPress, onClick)

    @Composable
    fun SearchListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyListState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        onClick: (ItemModel) -> Unit
    ) = ItemListView(list, favorites, listState, onLongPress, onClick)

    fun debugMenuItem(context: Context): List<@Composable LazyItemScope.() -> Unit> = emptyList()

}