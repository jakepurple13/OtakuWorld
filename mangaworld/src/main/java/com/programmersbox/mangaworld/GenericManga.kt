package com.programmersbox.mangaworld

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Environment
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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.datastore.createProtobuf
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.gsonutils.toJson
import com.programmersbox.helpfulutils.downloadManager
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.components.placeholder.M3PlaceHolderCoverCard
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.downloads.DownloadRoute
import com.programmersbox.manga.shared.downloads.DownloadScreen
import com.programmersbox.manga.shared.downloads.DownloadViewModel
import com.programmersbox.manga.shared.downloads.DownloadedMediaHandler
import com.programmersbox.manga.shared.onboarding.ReaderOnboarding
import com.programmersbox.manga.shared.reader.ReadView
import com.programmersbox.manga.shared.reader.ReadViewModel
import com.programmersbox.manga.shared.settings.ImageLoaderSettings
import com.programmersbox.manga.shared.settings.ImageLoaderSettingsRoute
import com.programmersbox.manga.shared.settings.PlayerSettings
import com.programmersbox.manga.shared.settings.ReaderSettings
import com.programmersbox.manga.shared.settings.ReaderSettingsScreen
import com.programmersbox.mangasettings.MangaNewSettingsHandling
import com.programmersbox.mangasettings.MangaNewSettingsSerializer
import com.programmersbox.mangaworld.reader.ReadActivity
import com.programmersbox.source_utilities.NetworkHelper
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ChapterModelSerializer
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.bindsGenericInfo
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import java.io.File

val appModule = module {
    singleOf(::GenericManga) { bindsGenericInfo() }

    singleOf(::NetworkHelper)
    single { NotificationLogo(R.drawable.manga_world_round_logo) }
    singleOf(::ChapterHolder)
    singleOf(::MangaSettingsHandling)
    single {
        MangaNewSettingsHandling(
            createProtobuf(
                context = get(),
                serializer = MangaNewSettingsSerializer,
                fileName = "MangaSettings.preferences_pb"
            )
        )
    }
    viewModelOf(::ReadViewModel)
    factoryOf(::DownloadedMediaHandler)
    viewModelOf(::DownloadViewModel)
}

//TODO: For multiplatform, maybe this becomes an open class that then the Android version overrides
// while ios and desktop just use the open class?
class GenericManga(
    val context: Context,
    val chapterHolder: ChapterHolder,
    val mangaSettingsHandling: MangaNewSettingsHandling,
    val settingsHandling: NewSettingsHandling,
    val appConfig: AppConfig,
) : GenericInfo {

    override val sourceType: String get() = "manga"

    override val deepLinkUri: String get() = "mangaworld://"

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> mangaNoFirebaseFile
                BuildType.NoCloudFirebase -> mangaNoCloudFile
                BuildType.Full -> mangaFile
            }
        }

    override val scrollBuffer: Int = 4

    override fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        chapterHolder.chapters = allChapters
        if (runBlocking { mangaSettingsHandling.useNewReader.flow.first() }) {
            chapterHolder.chapterModel = model
            ReadViewModel.navigateToMangaReader(
                navController,
                infoModel.title,
                model.url,
                model.sourceUrl
            )
        } else {
            context.startActivity(
                Intent(context, ReadActivity::class.java).apply {
                    putExtra("currentChapter", model.toJson(KmpChapterModel::class.java to ChapterModelSerializer()))
                    putExtra("allChapters", allChapters.toJson(KmpChapterModel::class.java to ChapterModelSerializer()))
                    putExtra("mangaTitle", infoModel.title)
                    putExtra("mangaUrl", model.url)
                    putExtra("mangaInfoUrl", model.sourceUrl)
                }
            )
        }
    }

    private fun downloadFullChapter(model: KmpChapterModel, title: String) {
        //val fileLocation = runBlocking { context.folderLocationFlow.first() }
        val fileLocation = DOWNLOAD_FILE_PATH

        val direct = File("$fileLocation$title/${model.name}/")
        if (!direct.exists()) direct.mkdir()

        GlobalScope.launch {
            model.getChapterInfo()
                .dispatchIo()
                .map { it.mapNotNull(KmpStorage::link) }
                .map {
                    it.mapIndexed { index, s ->
                        //val location = "/$fileLocation/$title/${model.name}"

                        //val file = File(Environment.getExternalStorageDirectory().path + location, "${String.format("%03d", index)}.png")

                        DownloadManager.Request(s.toUri())
                            //.setDestinationUri(file.toUri())
                            .setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                "MangaWorld/$title/${model.name}/${String.format("%03d", index)}"
                            )
                            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setAllowedOverRoaming(true)
                            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
                            .setMimeType("image/*")
                            .setTitle(model.name)
                            .addRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) Gecko/20100101 Firefox/77")
                            .addRequestHeader("Accept-Language", "en-US,en;q=0.5")
                    }
                }
                .onEach { it.fastForEach(context.downloadManager::enqueue) }
                .collect()
        }
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        /*activity.requestPermissions(
            *if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            else arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) { p -> if (p.isGranted) downloadFullChapter(model, infoModel.title.ifBlank { infoModel.url }) }*/

        downloadFullChapter(model, infoModel.title.ifBlank { infoModel.url })
    }

    @Composable
    override fun ComposeShimmerItem() {
        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) { items(10) { M3PlaceHolderCoverCard(placeHolder = painterResource(R.drawable.manga_world_round_logo)) } }
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
                    placeHolder = { painterResource(R.drawable.manga_world_round_logo) },
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
                    settingTitle = { Text(stringResource(R.string.downloaded_manga)) },
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

    @OptIn(
        ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class
    )
    context(navGraph: EntryProviderBuilder<NavKey>)
    override fun globalNav3Setup() {
        navGraph.entry<ReadViewModel.MangaReader> {
            ReadView(
                viewModel = koinViewModel { parametersOf(it) }
            )
        }
    }

    context(navGraph: EntryProviderBuilder<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<DownloadRoute> {
            DownloadScreen()
        }

        navGraph.entry<ImageLoaderSettingsRoute> {
            ImageLoaderSettings(mangaSettingsHandling)
        }

        navGraph.entry<ReaderSettingsScreen> {
            ReaderSettings(
                mangaSettingsHandling = mangaSettingsHandling,
                settingsHandling = settingsHandling
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