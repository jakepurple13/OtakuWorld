package com.programmersbox.novelworld

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.core.app.TaskStackBuilder
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
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
import com.programmersbox.novel.shared.ChapterHolder
import com.programmersbox.novel.shared.reader.NovelReadView
import com.programmersbox.novel.shared.reader.ReadViewModel
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.NotificationLogo
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.dsl.binds
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::GenericNovel) {
        binds(
            listOf(
                KmpGenericInfo::class,
                GenericInfo::class
            )
        )
    }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
    singleOf(::ChapterHolder)
    viewModelOf(::ReadViewModel)
}

class GenericNovel(
    val context: Context,
    val chapterHolder: ChapterHolder,
    val appConfig: AppConfig,
) : GenericInfo {

    override val deepLinkUri: String get() = "novelworld://"

    override val sourceType: String get() = "novel"

    override fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        chapterHolder.chapterModel = model
        ReadViewModel.navigateToNovelReader(
            navController,
            infoModel.title,
            model.url,
            model.sourceUrl
        )
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
    }

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> novelNoFirebaseFile
                BuildType.NoCloudFirebase -> novelNoCloudFile
                BuildType.Full -> novelFile
            }
        }

    @Composable
    override fun ComposeShimmerItem() {
        LazyColumn {
            items(10) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .m3placeholder(
                                true,
                                highlight = PlaceholderHighlight.shimmer()
                            )
                            .padding(4.dp)
                    )
                }
            }
        }
    }

    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class
    )
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
            modifier = modifier.fillMaxSize(),
        ) {
            items(list) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .combineClickableWithIndication(
                            onLongPress = { c -> onLongPress(it, c) },
                            onClick = { onClick(it) }
                        ),
                    tonalElevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                if (favorites.fastAny { f -> f.url == it.url }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        headlineContent = { Text(it.title) },
                        overlineContent = { Text(it.source.serviceName) },
                        supportingContent = if (it.description.isNotEmpty()) {
                            { Text(it.description) }
                        } else null
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    override fun EntryProviderBuilder<NavKey>.globalNav3Setup() {
        //TODO: Need to make sure this works
        /* entry<NovelReader>(
             //ReadViewModel.NovelReaderRoute,
             *//*metadata = mapOf(
                "currentChapter",
               "novelTitle",
               "novelUrl",
               "novelInfoUrl",
            )*//*
        ) {
            NovelReader()
        }*/
        entry<ReadViewModel.NovelReader> {
            NovelReadView(
                viewModel = koinViewModel { parametersOf(it) }
            )
        }
    }

    @OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    context(navGraph: NavGraphBuilder)
    override fun globalNavSetup() {
        /*composable(
            ReadViewModel.NovelReaderRoute,
            arguments = listOf(
                navArgument("currentChapter") { },
                navArgument("novelTitle") { },
                navArgument("novelUrl") { },
                navArgument("novelInfoUrl") { },
            )
        ) {
            NovelReader()
        }*/

        navGraph.composable<ReadViewModel.NovelReader>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            NovelReadView(
                viewModel = koinViewModel { parametersOf(it.toRoute<ReadViewModel.NovelReader>()) }
            )
        }
    }

    override fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkDetailsUri(itemModel),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(itemModel?.hashCode() ?: 0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun deepLinkSettings(context: Context): PendingIntent? {
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkSettingsUri(),
            context,
            MainActivity::class.java
        )

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(13, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}