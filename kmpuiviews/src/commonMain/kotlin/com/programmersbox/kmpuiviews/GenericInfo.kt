package com.programmersbox.kmpuiviews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Stable
sealed interface ChapterDownloadUiState {
    @Stable
    data object None : ChapterDownloadUiState

    @Stable
    data object Queued : ChapterDownloadUiState

    @Stable
    data class Downloading(val fraction: Float) : ChapterDownloadUiState

    @Stable
    data object Downloaded : ChapterDownloadUiState
}

interface KmpGenericInfo {
    val scrollBuffer: Int get() = 2
    val sourceType: String get() = ""

    val apkString: AppUpdate.AppUpdates.() -> String?

    suspend fun chapterOnClick(
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

    fun deleteDownloadedChapter(
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
    ) = Unit

    fun observeChapterDownloadStates(
        chapters: List<KmpChapterModel>,
        mangaTitle: String,
    ): Flow<Map<String, ChapterDownloadUiState>> = flowOf(emptyMap())

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

    context(navGraph: EntryProviderScope<NavKey>)
    fun globalNav3Setup() {
    }

    context(navGraph: EntryProviderScope<NavKey>)
    fun settingsNav3Setup() {
    }

    @Composable
    fun DialogSetups() = Unit

    @Composable
    fun AccountContent() = Unit

    @Composable
    fun AccountSettings() = Unit

    @Composable
    fun ProfileIcon(): String
}

expect interface PlatformGenericInfo : KmpGenericInfo