package com.programmersbox.anime.shared

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import com.programmersbox.anime.shared.videochoice.VideoSourceModel
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.components.placeholder.PlaceholderHighlight
import com.programmersbox.kmpuiviews.presentation.components.placeholder.m3placeholder
import com.programmersbox.kmpuiviews.presentation.components.placeholder.shimmer
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.composables.modifiers.combineClickableWithIndication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull

abstract class GenericSharedAnime(
    protected val appConfig: AppConfig,
) : KmpGenericInfo {

    override val sourceType: String get() = "anime"

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> animeNoFirebaseFile
                BuildType.Full -> animeFile
            }
        }

    var isLoadingChapter by mutableStateOf(false)
        private set

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        if (isLoadingChapter) return
        isLoadingChapter = true
        try {
            val storages = model.getChapterInfo().firstOrNull().orEmpty()

            when {
                storages.size == 1 -> playResolved(navController, storages.first(), model, infoModel)
                storages.isNotEmpty() -> VideoSourceModel.showVideoSources = VideoSourceModel(
                    c = storages,
                    infoModel = infoModel,
                    isStreaming = true,
                    model = model,
                )
                else -> Unit
            }
        } finally {
            isLoadingChapter = false
        }
    }

    /**
     * Download counterpart of [chapterOnClick]: same loading guard, one storage downloads directly,
     * several go to the picker, none or an error calls [onNone].
     */
    suspend fun downloadOnClick(
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        onNone: suspend () -> Unit,
        download: suspend (KmpStorage) -> Unit,
    ) {
        if (isLoadingChapter) return
        isLoadingChapter = true
        try {
            val storages = try {
                model.getChapterInfo().firstOrNull().orEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }

            when {
                storages.size == 1 -> download(storages.first())
                storages.isNotEmpty() -> VideoSourceModel.showVideoSources = VideoSourceModel(
                    c = storages,
                    infoModel = infoModel,
                    isStreaming = false,
                    model = model,
                )
                else -> onNone()
            }
        } finally {
            isLoadingChapter = false
        }
    }

    /** Lets the source resolve a lazily-described stream first; on failure nothing plays. */
    suspend fun playResolved(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
    ) {
        val resolved = try {
            model.source.resolve(storage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            onResolveFailed(storage, e)
            return
        }
        playOrCast(navController, resolved, model, infoModel)
    }

    /** Called when resolving a stream fails, so the platform can tell the user. */
    open fun onResolveFailed(storage: KmpStorage, error: Throwable) {}

    abstract fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
    )

    @Composable
    override fun ComposeShimmerItem() {
        LazyColumn {
            items(10) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .m3placeholder(
                                true,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                    ) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Text(
                            "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun ItemListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = paddingValues,
            modifier = modifier.fillMaxSize()
        ) {
            items(list) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .combineClickableWithIndication(
                            onLongPress = { c -> onLongPress(it, c) },
                            onClick = { onClick(it) }
                        )
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                if (favorites.fastAny { f -> f.url == it.url }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        headlineContent = { Text(it.title) },
                        overlineContent = { Text(it.source.displayName) },
                        supportingContent = if (it.description.isNotEmpty()) {
                            { Text(it.description) }
                        } else null
                    )
                }
            }
        }
    }
}
