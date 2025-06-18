package com.programmersbox.desktop

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl

class GenericMangaDesktop(
    val settingsHandling: NewSettingsHandling,
) : KmpGenericInfo {

    override val apkString: AppUpdate.AppUpdates.() -> String? = { "" }

    override val sourceType: String get() = "manga"


    override val scrollBuffer: Int = 4

    override fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        /*chapterHolder.chapters = allChapters
        chapterHolder.chapterModel = model
        ReadViewModel.navigateToMangaReader(
            navController,
            infoModel.title,
            model.url,
            model.sourceUrl
        )*/
    }

    @Composable
    override fun ComposeShimmerItem() {
        /*LazyVerticalGrid(
            columns = adaptiveGridCell(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) { items(10) { M3PlaceHolderCoverCard(placeHolder = R.drawable.manga_world_round_logo) } }*/
    }

    @OptIn(
        ExperimentalFoundationApi::class
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
        /*LazyVerticalGrid(
            columns = adaptiveGridCell(),
            state = listState,
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                list,
                key = { i, it -> "${it.url}$i" },
                contentType = { _, i -> i }
            ) { _, it ->
                M3CoverCard(
                    onLongPress = { c -> onLongPress(it, c) },
                    imageUrl = it.imageUrl,
                    name = it.title,
                    headers = it.extras,
                    placeHolder = R.drawable.manga_world_round_logo,
                    favoriteIcon = {
                        if (favorites.any { f -> f.url == it.url }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        }
                    },
                    modifier = Modifier.animateItem()
                ) { onClick(it) }
            }
        }*/
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        /*viewSettings {
            val navController = LocalNavController.current
            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.downloaded_manga)) },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null
                ) { navController.navigate(DownloadViewModel.DownloadRoute) { launchSingleTop = true } }
            )

            PreferenceSetting(
                settingTitle = { Text("Manga Reader Settings") },
                settingIcon = { Icon(Icons.AutoMirrored.Filled.ChromeReaderMode, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = ripple(),
                    interactionSource = null
                ) { navController.navigate(ReaderSettingsScreen) { launchSingleTop = true } }
            )
        }

        generalSettings {

        }

        playerSettings {
            PlayerSettings(
                mangaSettingsHandling = mangaSettingsHandling,
            )
        }

        onboardingSettings {
            item {
                ReaderOnboarding(
                    mangaSettingsHandling = mangaSettingsHandling,
                )
            }
        }*/
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {

    }

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
    )
    override fun NavGraphBuilder.globalNavSetup() {
        /*composable<ReadViewModel.MangaReader>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            trackScreen("mangaReader")
            ReadView()
        }*/
    }

    override fun NavGraphBuilder.settingsNavSetup() {
        /*composable(
            DownloadViewModel.DownloadRoute,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
        ) {
            trackScreen(DownloadViewModel.DownloadRoute)
            DownloadScreen()
        }

        composable(
            ImageLoaderSettingsRoute,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
        ) {
            trackScreen(ImageLoaderSettingsRoute)
            ImageLoaderSettings(mangaSettingsHandling)
        }

        composable<ReaderSettingsScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
        ) {
            trackScreen("readerSettings")
            ReaderSettings(
                mangaSettingsHandling = mangaSettingsHandling,
                settingsHandling = settingsHandling
            )
        }*/
    }
}