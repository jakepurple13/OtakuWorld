package com.programmersbox.uiviews

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.serialization.generateRouteWithArgs
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.uiviews.presentation.settings.ComposeSettingsDsl
import com.programmersbox.uiviews.utils.ComponentState

interface GenericInfo {

    val apkString: AppUpdate.AppUpdates.() -> String?
    val scrollBuffer: Int get() = 2
    val deepLinkUri: String

    val sourceType: String get() = ""

    //TODO: Would need to be different
    // Probably pass a context into the constructor?
    fun deepLinkDetails(context: Context, itemModel: ItemModel?): PendingIntent?

    //TODO: Would need to be different
    fun deepLinkSettings(context: Context): PendingIntent?

    @SuppressLint("RestrictedApi")
    fun deepLinkDetailsUri(itemModel: ItemModel?): Uri {
        @Suppress("UNCHECKED_CAST")
        val route = generateRouteWithArgs(
            Screen.DetailsScreen.Details(
                title = itemModel?.title ?: "",
                description = itemModel?.description ?: "",
                url = itemModel?.url ?: "",
                imageUrl = itemModel?.imageUrl ?: "",
                source = itemModel?.source?.serviceName ?: "",
            ),
            mapOf(
                "title" to NavType.StringType as NavType<Any?>,
                "description" to NavType.StringType as NavType<Any?>,
                "url" to NavType.StringType as NavType<Any?>,
                "imageUrl" to NavType.StringType as NavType<Any?>,
                "source" to NavType.StringType as NavType<Any?>,
            )
        )

        return "$deepLinkUri$route".toUri()
    }

    fun deepLinkSettingsUri() = "$deepLinkUri${Screen.NotificationScreen.route}".toUri()

    //TODO: Would need to be different
    fun chapterOnClick(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController,
    )

    fun sourceList(): List<ApiService>
    fun searchList(): List<ApiService> = sourceList()

    //TODO: Can be removed
    fun toSource(s: String): ApiService?
    fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {}

    //TODO: Would need to be different
    fun downloadChapter(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController,
    )

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
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (ItemModel) -> Unit,
    )

    @ExperimentalFoundationApi
    @Composable
    fun AllListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (ItemModel) -> Unit,
    ) = ItemListView(list, favorites, listState, onLongPress, modifier, paddingValues, onClick)

    @ExperimentalFoundationApi
    @Composable
    fun SearchListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (ItemModel) -> Unit,
    ) = ItemListView(list, favorites, listState, onLongPress, modifier, paddingValues, onClick)

    //TODO: Would need to be different
    fun debugMenuItem(context: Context): List<@Composable LazyItemScope.() -> Unit> = emptyList()

    fun NavGraphBuilder.globalNavSetup() = Unit

    fun NavGraphBuilder.settingsNavSetup(): Unit = Unit

    @Composable
    fun DialogSetups() = Unit
}