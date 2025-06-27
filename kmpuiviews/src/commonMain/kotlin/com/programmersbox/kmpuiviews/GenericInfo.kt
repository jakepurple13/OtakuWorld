package com.programmersbox.kmpuiviews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl

interface KmpGenericInfo {
    val scrollBuffer: Int get() = 2
    val sourceType: String get() = ""

    val apkString: AppUpdate.AppUpdates.() -> String?

    fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    )

    fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {}

    fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    )

    @Composable
    fun DetailActions(infoModel: KmpInfoModel, tint: Color) = Unit

    @Composable
    fun ComposeShimmerItem()

    @ExperimentalFoundationApi
    @Composable
    fun ItemListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    )

    @ExperimentalFoundationApi
    @Composable
    fun AllListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    ) = ItemListView(list, favorites, listState, onLongPress, modifier, paddingValues, onClick)

    @ExperimentalFoundationApi
    @Composable
    fun SearchListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    ) = ItemListView(list, favorites, listState, onLongPress, modifier, paddingValues, onClick)

    fun debugMenuItem(): List<@Composable LazyItemScope.() -> Unit> = emptyList()

    context(navGraph: NavGraphBuilder)
    fun globalNavSetup() = Unit

    context(navGraph: NavGraphBuilder)
    fun settingsNavSetup(): Unit = Unit

    @Composable
    fun DialogSetups() = Unit
}