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
import com.programmersbox.kmpmodels.KmpApiService
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.presentation.Screen
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
    fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent?

    //TODO: Would need to be different
    fun deepLinkSettings(context: Context): PendingIntent?

    @SuppressLint("RestrictedApi")
    fun deepLinkDetailsUri(itemModel: KmpItemModel?): Uri {
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
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController,
    )

    fun sourceList(): List<KmpApiService>
    fun searchList(): List<KmpApiService> = sourceList()

    //TODO: Can be removed
    fun toSource(s: String): KmpApiService?
    fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {}

    //TODO: Would need to be different
    fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController,
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

    //TODO: Would need to be different
    fun debugMenuItem(context: Context): List<@Composable LazyItemScope.() -> Unit> = emptyList()

    fun NavGraphBuilder.globalNavSetup() = Unit

    fun NavGraphBuilder.settingsNavSetup(): Unit = Unit

    @Composable
    fun DialogSetups() = Unit
}