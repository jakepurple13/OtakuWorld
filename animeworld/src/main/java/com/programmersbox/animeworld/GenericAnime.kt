package com.programmersbox.animeworld

import android.Manifest
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PersonalVideo
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.TaskStackBuilder
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.app.MediaRouteDialogFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.google.android.gms.cast.framework.CastContext
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.obsez.android.lib.filechooser.ChooserDialog
import com.programmersbox.animeworld.cast.ExpandedControlsActivity
import com.programmersbox.animeworld.videochoice.VideoChoiceScreen
import com.programmersbox.animeworld.videochoice.VideoSourceModel
import com.programmersbox.animeworld.videoplayer.VideoPlayerUi
import com.programmersbox.animeworld.videoplayer.VideoViewModel
import com.programmersbox.animeworld.videos.ViewVideoScreen
import com.programmersbox.animeworld.videos.ViewVideoViewModel
import com.programmersbox.datastore.asState
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.helpfulutils.downloadManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
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
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavController
import com.programmersbox.kmpuiviews.utils.composables.modifiers.combineClickableWithIndication
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.trackScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.module.dsl.binds
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::GenericAnime) {
        binds(
            listOf(
                KmpGenericInfo::class,
                GenericInfo::class
            )
        )
    }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
    single { StorageHolder() }
    single { AnimeDataStoreHandling() }
}

class StorageHolder {
    var storageModel: KmpStorage? = null
}

class GenericAnime(
    val context: Context,
    val storageHolder: StorageHolder,
    val animeDataStoreHandling: AnimeDataStoreHandling,
    val appConfig: AppConfig,
) : GenericInfo {

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> animeNoFirebaseFile
                BuildType.NoCloudFirebase -> animeNoCloudFile
                BuildType.Full -> animeFile
            }
        }
    override val deepLinkUri: String get() = "animeworld://"

    override val sourceType: String get() = "anime"

    override fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavController,
    ) {
        /*if ((model.source as? ShowApi)?.canPlay == false) {
            Toast.makeText(context, context.getString(R.string.source_no_stream, model.source.serviceName), Toast.LENGTH_SHORT).show()
            return
        }*/
        getEpisodes(
            errorId = R.string.source_no_stream,
            model = model,
            infoModel = infoModel,
            context = context,
            navController = navController,
            isStreaming = true,
        ) {
            if (MainActivity.cast.isCastActive()) {
                MainActivity.cast.loadUrl(
                    it.link,
                    infoModel.title,
                    model.name,
                    infoModel.imageUrl,
                    it.headers
                )
            } else {
                storageHolder.storageModel = it
                context.navigateToVideoPlayer(
                    navController,
                    it.link,
                    model.name,
                    false,
                    it.headers["referer"] ?: it.source.orEmpty()
                )
            }
        }
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavController,
    ) {
        /* if ((model.source as? ShowApi)?.canDownload == false) {
             Toast.makeText(
                 context,
                 context.getString(R.string.source_no_download, model.source.serviceName),
                 Toast.LENGTH_SHORT
             ).show()
             return
         }*/
        /*activity.requestPermissions(
            *if (Build.VERSION.SDK_INT >= 33)
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            else arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        ) { p ->
            if (p.isGranted) {
                Toast.makeText(context, R.string.downloading_dots_no_percent, Toast.LENGTH_SHORT).show()
                getEpisodes(
                    errorId = R.string.source_no_download,
                    model = model,
                    infoModel = infoModel,
                    context = context,
                    filter = { !it.link.orEmpty().endsWith(".m3u8") },
                    navController = navController,
                    isStreaming = false
                ) {
                    //fetchIt(it, model, activity)
                    downloadVideo(context, model, it)
                }
            }
        }*/
        Toast.makeText(context, R.string.downloading_dots_no_percent, Toast.LENGTH_SHORT).show()
        getEpisodes(
            errorId = R.string.source_no_download,
            model = model,
            infoModel = infoModel,
            context = context,
            filter = { !it.link.orEmpty().endsWith(".m3u8") },
            navController = navController,
            isStreaming = false
        ) {
            //fetchIt(it, model, activity)
            downloadVideo(context, model, it)
        }
    }

    private fun getEpisodes(
        errorId: Int,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        context: Context,
        filter: (KmpStorage) -> Boolean = { true },
        navController: NavController,
        isStreaming: Boolean,
        onAction: (KmpStorage) -> Unit,
    ) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.loading_please_wait)
            .setView(ProgressBar(context))
            .setIcon(R.mipmap.ic_launcher)
            .setCancelable(false)
            .create()
        CoroutineScope(Job() + Dispatchers.IO).launch {
            model.getChapterInfo()
                .onStart { withContext(Dispatchers.Main) { dialog.show() } }
                .catch {
                    runOnUIThread {
                        Toast.makeText(context, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    emit(emptyList())
                }
                .map { it.filter(filter) }
                .collect { c ->
                    dialog.dismiss()
                    when {
                        c.size == 1 -> {
                            when (model.source) {
                                else -> c.firstOrNull()?.let { onAction(it) }
                            }
                        }

                        c.isNotEmpty() -> {
                            VideoSourceModel.showVideoSources = VideoSourceModel(
                                c = c,
                                infoModel = infoModel,
                                isStreaming = isStreaming,
                                model = model
                            )
                        }

                        else -> {
                            Toast.makeText(context, context.getString(errorId, model.source.serviceName), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    fun downloadVideo(context: Context, model: KmpChapterModel, storage: KmpStorage) {
        fun getNameFromUrl(url: String): String {
            return Uri.parse(url).lastPathSegment?.let { it.ifEmpty { model.name } } ?: model.name
        }

        val d = DownloadManager.Request(storage.link!!.toUri())
            .setDestinationUri((context.folderLocation + getNameFromUrl(storage.link!!) + "${model.name}.mp4").toUri())
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverRoaming(true)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            .setMimeType("image/*")
            .setTitle(model.name)
            .addRequestHeader("Accept-Language", "en-US,en;q=0.5")
            .addRequestHeader("User-Agent", "\"Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0\"")
            .addRequestHeader("Accept", "text/html,video/mp4,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addRequestHeader("Access-Control-Allow-Origin", "*")
            .addRequestHeader("Referer", storage.headers["referer"] ?: "http://thewebsite.com")
            .addRequestHeader("Connection", "keep-alive")
            .apply {
                storage.headers.forEach { (t, u) ->
                    addRequestHeader(t, u)
                }
            }

        runCatching { context.downloadManager.enqueue(d) }
            .onSuccess { Toast.makeText(context, "Downloading...", Toast.LENGTH_SHORT).show() }
            .onFailure {
                it.printStackTrace()
                Toast.makeText(context, "Something went wrong...", Toast.LENGTH_SHORT).show()
            }
    }

    @Composable
    override fun DetailActions(infoModel: KmpInfoModel, tint: Color) {
        val showCast by MainActivity.cast.sessionConnected()
            .collectAsStateWithLifecycle(true)

        AnimatedVisibility(
            visible = showCast,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
            AndroidView(
                modifier = Modifier.animateContentSize(),
                factory = { context ->
                    MediaRouteButton(context).apply {
                        MainActivity.cast.showIntroductoryOverlay(this)
                        MainActivity.cast.setMediaRouteMenu(context, this)
                    }
                }
            )
        }
    }

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

    @OptIn(
        ExperimentalFoundationApi::class,
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
                        overlineContent = { Text(it.source.serviceName) },
                        supportingContent = if (it.description.isNotEmpty()) {
                            { Text(it.description) }
                        } else null
                    )
                }
            }
        }
    }

    class CastingViewModel : ViewModel() {

        var connection by mutableStateOf(false)
        var session by mutableStateOf(false)

        init {
            viewModelScope.launch { MainActivity.cast.sessionConnected().onEach { connection = it }.collect() }
            viewModelScope.launch { MainActivity.cast.sessionStatus().onEach { session = it }.collect() }
        }
    }

    override fun composeCustomPreferences(): ComposeSettingsDsl.() -> Unit = {
        viewSettings {

            item {
                val navController = LocalNavController.current
                PreferenceSetting(
                    settingTitle = { Text(stringResource(R.string.video_menu_title)) },
                    settingIcon = { Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.navigate(ViewVideoViewModel.VideoViewerRoute) { launchSingleTop = true } }
                )
            }


            item {
                val context = LocalContext.current
                val castingViewModel: CastingViewModel = viewModel()
                val activity = LocalActivity.current
                ShowWhen(castingViewModel.connection) {
                    PreferenceSetting(
                        settingTitle = { Text(stringResource(R.string.cast_menu_title)) },
                        settingIcon = {
                            Icon(
                                if (castingViewModel.session) Icons.Default.CastConnected else Icons.Default.Cast,
                                null,
                                modifier = Modifier.fillMaxSize()
                            )
                        },
                        modifier = Modifier.clickable(
                            indication = ripple(),
                            interactionSource = null
                        ) {
                            if (MainActivity.cast.isCastActive()) {
                                context.startActivity(Intent(context, ExpandedControlsActivity::class.java))
                            } else {
                                (activity as? FragmentActivity)?.supportFragmentManager?.let {
                                    MediaRouteDialogFactory.getDefault().onCreateChooserDialogFragment()
                                        .also { m ->
                                            CastContext.getSharedInstance(context) {}.result.mergedSelector?.let { m.routeSelector = it }
                                        }
                                        .show(it, "media_chooser")
                                }
                            }
                        }
                    )
                }
            }
        }

        generalSettings {
            val context = LocalContext.current
            var folderLocation by remember { mutableStateOf(context.folderLocation) }
            val activity = LocalActivity.current

            CategoryGroup {
                item {
                    PreferenceSetting(
                        settingTitle = { Text(stringResource(R.string.folder_location)) },
                        summaryValue = { Text(folderLocation) },
                        settingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.fillMaxSize()) },
                        modifier = Modifier.clickable(
                            indication = ripple(),
                            interactionSource = null
                        ) {
                            (activity as? FragmentActivity)?.requestPermissions(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            ) {
                                if (it.isGranted) {
                                    ChooserDialog(context)
                                        .withIcon(R.mipmap.ic_launcher)
                                        .withResources(R.string.choose_a_directory, R.string.chooseText, R.string.cancelText)
                                        .withFilter(true, false)
                                        .withStartFile(context.folderLocation)
                                        .enableOptions(true)
                                        .withChosenListener { dir, _ ->
                                            context.folderLocation = "$dir/"
                                            println(dir)
                                            folderLocation = context.folderLocation
                                        }
                                        .build()
                                        .show()
                                }
                            }
                        }
                    )
                }
            }
        }

        playerSettings {
            val scope = rememberCoroutineScope()
            val useNewPlayer = animeDataStoreHandling.useNewPlayer
            val player by useNewPlayer.asState()
            val ignoreSsl = animeDataStoreHandling.ignoreSsl
            val ignoreSslState by ignoreSsl.asState()

            CategoryGroup {
                item {
                    SwitchSetting(
                        settingTitle = { Text(stringResource(R.string.use_new_player)) },
                        summaryValue = { Text(stringResource(R.string.use_new_player_description)) },
                        settingIcon = { Icon(Icons.Default.PersonalVideo, null, modifier = Modifier.fillMaxSize()) },
                        value = player,
                        updateValue = { scope.launch { useNewPlayer.set(it) } }
                    )
                }

                item {
                    SwitchSetting(
                        settingTitle = { Text(stringResource(id = R.string.ignore_ssl)) },
                        settingIcon = { Icon(Icons.Default.Security, null, modifier = Modifier.fillMaxSize()) },
                        value = ignoreSslState
                    ) { scope.launch { ignoreSsl.set(it) } }
                }
            }
        }
    }

    override fun NavGraphBuilder.globalNavSetup() {
        composable(
            VideoViewModel.VideoPlayerRoute,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
        ) {
            trackScreen("video_player")
            VideoPlayerUi()
        }
    }

    override fun NavGraphBuilder.settingsNavSetup() {
        composable(
            ViewVideoViewModel.VideoViewerRoute,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
            deepLinks = listOf(navDeepLink { uriPattern = "animeworld://${ViewVideoViewModel.VideoViewerRoute}" })
        ) {
            trackScreen(ViewVideoViewModel.VideoViewerRoute)
            ViewVideoScreen()
        }
    }

    @Composable
    override fun DialogSetups() {
        VideoSourceModel.showVideoSources?.let {
            VideoChoiceScreen(
                items = it.c,
                infoModel = it.infoModel,
                isStreaming = it.isStreaming,
                model = it.model
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
