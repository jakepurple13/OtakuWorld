package com.programmersbox.uiviews

import android.app.PendingIntent
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.uiviews.settings.ComposeSettingsDsl
import com.programmersbox.uiviews.utils.ComponentState

interface GenericInfo {

    val apkString: AppUpdate.AppUpdates.() -> String?
    val scrollBuffer: Int get() = 2
    val deepLinkUri: String

    fun deepLinkDetails(context: Context, itemModel: ItemModel?): PendingIntent?
    fun deepLinkSettings(context: Context): PendingIntent?

    fun chapterOnClick(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController
    )

    fun sourceList(): List<ApiService>
    fun searchList(): List<ApiService> = sourceList()
    fun toSource(s: String): ApiService?
    fun composeCustomPreferences(navController: NavController): ComposeSettingsDsl.() -> Unit = {}
    fun downloadChapter(model: ChapterModel, allChapters: List<ChapterModel>, infoModel: InfoModel, context: Context, activity: FragmentActivity)

    @Composable
    fun DetailActions(infoModel: InfoModel, tint: Color) = Unit

    @Composable
    fun ComposeShimmerItem()

    @ExperimentalFoundationApi
    @Composable
    fun ItemListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        onClick: (ItemModel) -> Unit
    )

    @ExperimentalFoundationApi
    @Composable
    fun AllListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        onClick: (ItemModel) -> Unit
    ) = ItemListView(list, favorites, listState, onLongPress, onClick)

    @ExperimentalFoundationApi
    @Composable
    fun SearchListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        onClick: (ItemModel) -> Unit
    ) = ItemListView(list, favorites, listState, onLongPress, onClick)

    fun debugMenuItem(context: Context): List<@Composable LazyItemScope.() -> Unit> = emptyList()

    fun recentNavSetup(fragment: Fragment, navController: NavController) = Unit
    fun allNavSetup(fragment: Fragment, navController: NavController) = Unit
    fun settingNavSetup(fragment: Fragment, navController: NavController) = Unit

    fun NavGraphBuilder.navSetup() = Unit

    @Composable
    fun BottomBarAdditions() = Unit
}