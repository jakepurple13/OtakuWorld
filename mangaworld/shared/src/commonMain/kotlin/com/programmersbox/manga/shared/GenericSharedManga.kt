package com.programmersbox.manga.shared

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ChromeReaderMode
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.components.placeholder.M3PlaceHolderCoverCard
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.manga.shared.downloads.DownloadRoute
import com.programmersbox.manga.shared.downloads.DownloadScreen
import com.programmersbox.manga.shared.onboarding.ReaderOnboarding
import com.programmersbox.manga.shared.reader.ReadView
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.manga.shared.settings.ImageLoaderSettings
import com.programmersbox.manga.shared.settings.ImageLoaderSettingsRoute
import com.programmersbox.manga.shared.settings.PlayerSettings
import com.programmersbox.manga.shared.settings.ReaderSettings
import com.programmersbox.manga.shared.settings.ReaderSettingsScreen
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

abstract class GenericSharedManga(
    val mangaSettingsHandling: MangaNewSettingsHandling,
    val settingsHandling: NewSettingsHandling,
    val appConfig: AppConfig,
) : KmpGenericInfo {

    override val sourceType: String get() = "manga"

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> mangaNoFirebaseFile
                BuildType.NoCloudFirebase -> mangaNoCloudFile
                BuildType.Full -> mangaFile
            }
        }

    override val scrollBuffer: Int = 4

    @Composable
    override fun ComposeShimmerItem() {
        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) { items(10) { M3PlaceHolderCoverCard(placeHolder = painterLogo()) } }
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
        val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
        val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }

        LazyVerticalGrid(
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
                    placeHolder = { painterLogo() },
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
                    colorFilter = colorFilter,
                    modifier = Modifier.animateItem()
                ) { onClick(it) }
            }
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        viewSettings {
            item {
                val navController = LocalNavActions.current
                PreferenceSetting(
                    settingTitle = { Text("Downloads") },
                    settingIcon = { Icon(Icons.AutoMirrored.Filled.LibraryBooks, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.navigate(DownloadRoute) }
                )
            }

            item {
                val navController = LocalNavActions.current
                PreferenceSetting(
                    settingTitle = { Text("Manga Reader Settings") },
                    settingIcon = { Icon(Icons.AutoMirrored.Filled.ChromeReaderMode, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.navigate(ReaderSettingsScreen) }
                )
            }
        }

        generalSettings {
            /*if (BuildConfig.DEBUG) {

                val folderLocation by context.folderLocationFlow.collectAsState(initial = DOWNLOAD_FILE_PATH)

                val folderIntent = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                    uri?.path?.removePrefix("/tree/primary:")?.let {
                        //context.folderLocation = "$it/"
                        scope.launch { context.updatePref(FOLDER_LOCATION, it) }
                        println(it)
                    }
                }

                val storagePermissions = rememberMultiplePermissionsState(
                    listOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    )
                )

                var resetFolderDialog by remember { mutableStateOf(false) }

                if (resetFolderDialog) {
                    AlertDialog(
                        onDismissRequest = { resetFolderDialog = false },
                        title = { Text("Reset Folder Location to") },
                        text = { Text(DOWNLOAD_FILE_PATH) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    scope.launch { context.updatePref(FOLDER_LOCATION, DOWNLOAD_FILE_PATH) }
                                    resetFolderDialog = false
                                }
                            ) { Text("Reset") }
                        },
                        dismissButton = { TextButton(onClick = { resetFolderDialog = false }) { Text(stringResource(R.string.cancel)) } }
                    )
                }

                PermissionsRequired(
                    multiplePermissionsState = storagePermissions,
                    permissionsNotGrantedContent = {
                        PreferenceSetting(
                            endIcon = {
                                IconButton(onClick = { resetFolderDialog = true }) {
                                    androidx.compose.material3.Icon(Icons.Default.FolderDelete, null)
                                }
                            },
                            settingTitle = { androidx.compose.material3.Text(stringResource(R.string.folder_location)) },
                            summaryValue = { androidx.compose.material3.Text(folderLocation) },
                            settingIcon = { androidx.compose.material3.Icon(Icons.Default.Folder, null, modifier = Modifier.fillMaxSize()) },
                            modifier = Modifier.clickable(
                                indication = rememberRipple(),
                                interactionSource = null
                            ) { storagePermissions.launchMultiplePermissionRequest() }
                        )
                    },
                    permissionsNotAvailableContent = {
                        NeedsPermissions {
                            context.startActivity(
                                Intent().apply {
                                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    }
                ) {
                    PreferenceSetting(
                        endIcon = {
                            IconButton(onClick = { resetFolderDialog = true }) {
                                androidx.compose.material3.Icon(Icons.Default.FolderDelete, null)
                            }
                        },
                        settingTitle = { androidx.compose.material3.Text(stringResource(R.string.folder_location)) },
                        summaryValue = { androidx.compose.material3.Text(folderLocation) },
                        settingIcon = { androidx.compose.material3.Icon(Icons.Default.Folder, null, modifier = Modifier.fillMaxSize()) },
                        modifier = Modifier.clickable(
                            indication = rememberRipple(),
                            interactionSource = null
                        ) {
                            if (storagePermissions.allPermissionsGranted) {
                                folderIntent.launch(folderLocation.toUri())
                            } else {
                                storagePermissions.launchMultiplePermissionRequest()
                            }
                        }
                    )
                }
            }*/
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
        }
    }

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
    )
    context(navGraph: NavGraphBuilder)
    override fun globalNavSetup() {
        navGraph.composable<ReadViewModel.MangaReader>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            ReadView(
                viewModel = koinViewModel { parametersOf(it.toRoute<ReadViewModel.MangaReader>()) }
            )
        }
    }

    context(navGraph: NavGraphBuilder)
    override fun settingsNavSetup() {
        navGraph.composable<DownloadRoute>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
        ) {
            DownloadScreen()
        }

        navGraph.composable<ImageLoaderSettingsRoute>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
        ) {
            ImageLoaderSettings(mangaSettingsHandling)
        }

        navGraph.composable<ReaderSettingsScreen>(
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
        ) {
            ReaderSettings(
                mangaSettingsHandling = mangaSettingsHandling,
                settingsHandling = settingsHandling
            )
        }
    }
}