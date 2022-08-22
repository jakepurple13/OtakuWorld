package com.programmersbox.animeworld

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.ProgressBar
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.TaskStackBuilder
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.app.MediaRouteDialogFactory
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.navDeepLink
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer
import com.google.android.gms.cast.framework.CastContext
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.obsez.android.lib.filechooser.ChooserDialog
import com.programmersbox.anime_sources.ShowApi
import com.programmersbox.anime_sources.Sources
import com.programmersbox.anime_sources.utilities.Qualities
import com.programmersbox.anime_sources.utilities.getQualityFromName
import com.programmersbox.animeworld.cast.ExpandedControlsActivity
import com.programmersbox.animeworld.downloads.DownloaderUi
import com.programmersbox.animeworld.downloads.DownloaderViewModel
import com.programmersbox.animeworld.videoplayer.VideoPlayerUi
import com.programmersbox.animeworld.videoplayer.VideoViewModel
import com.programmersbox.animeworld.videos.ViewVideoScreen
import com.programmersbox.animeworld.videos.ViewVideoViewModel
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.*
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.sharedutils.MainLogo
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.settings.ComposeSettingsDsl
import com.programmersbox.uiviews.utils.*
import com.programmersbox.uiviews.utils.components.ListBottomSheet
import com.programmersbox.uiviews.utils.components.ListBottomSheetItemModel
import com.tonyodev.fetch2.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.dsl.module
import kotlin.collections.set

val appModule = module {
    single<GenericInfo> { GenericAnime(get()) }
    single { MainLogo(R.mipmap.ic_launcher) }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
}

class GenericAnime(val context: Context) : GenericInfo {

    override val apkString: AppUpdate.AppUpdates.() -> String? get() = { if (BuildConfig.FLAVOR == "noFirebase") anime_no_firebase_file else anime_file }
    override val deepLinkUri: String get() = "animeworld://"

    override fun chapterOnClick(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController
    ) {
        if ((model.source as? ShowApi)?.canPlay == false) {
            Toast.makeText(context, context.getString(R.string.source_no_stream, model.source.serviceName), Toast.LENGTH_SHORT).show()
            return
        }
        getEpisodes(
            R.string.source_no_stream,
            model,
            context,
            activity,
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

    private val fetch = Fetch.getDefaultInstance()

    override fun downloadChapter(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity
    ) {
        if ((model.source as? ShowApi)?.canDownload == false) {
            Toast.makeText(
                context,
                context.getString(R.string.source_no_download, model.source.serviceName),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        activity.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE) { p ->
            if (p.isGranted) {
                Toast.makeText(context, R.string.downloading_dots_no_percent, Toast.LENGTH_SHORT).show()
                getEpisodes(
                    R.string.source_no_download,
                    model,
                    context,
                    activity,
                    { !it.link.orEmpty().endsWith(".m3u8") }
                ) { fetchIt(it, model, activity) }
            }
        }
    }

    private fun fetchIt(i: Storage, ep: ChapterModel, activity: FragmentActivity) {
        try {
            fetch.setGlobalNetworkType(NetworkType.ALL)

            fun getNameFromUrl(url: String): String {
                return Uri.parse(url).lastPathSegment?.let { it.ifEmpty { ep.name } } ?: ep.name
            }

            val requestList = arrayListOf<Request>()

            val filePath = activity.folderLocation + getNameFromUrl(i.link!!) + "${ep.name}.mp4"
            val request = Request(i.link!!, filePath)
            request.priority = Priority.HIGH
            request.networkType = NetworkType.ALL
            request.enqueueAction = EnqueueAction.REPLACE_EXISTING
            request.extras.map.toProperties()["URL_INTENT"] = ep.url
            request.extras.map.toProperties()["NAME_INTENT"] = ep.name

            request.addHeader("Accept-Language", "en-US,en;q=0.5")
            request.addHeader("User-Agent", "\"Mozilla/5.0 (Windows NT 10.0; WOW64; rv:40.0) Gecko/20100101 Firefox/40.0\"")
            request.addHeader("Accept", "text/html,video/mp4,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            request.addHeader("Access-Control-Allow-Origin", "*")
            request.addHeader("Referer", i.headers["referer"] ?: "http://thewebsite.com")
            request.addHeader("Connection", "keep-alive")

            i.headers.entries.forEach { request.headers[it.key] = it.value }

            requestList.add(request)

            fetch.enqueue(requestList) {}
        } catch (e: Exception) {
            activity.runOnUiThread { Toast.makeText(activity, R.string.something_went_wrong, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun getEpisodes(
        errorId: Int,
        model: ChapterModel,
        context: Context,
        activity: FragmentActivity,
        filter: (Storage) -> Boolean = { true },
        onAction: (Storage) -> Unit
    ) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.loading_please_wait)
            .setView(ProgressBar(context))
            .setIcon(R.mipmap.ic_launcher)
            .setCancelable(false)
            .create()
        activity.lifecycleScope.launch {
            model.getChapterInfo()
                .onStart { runOnUIThread { dialog.show() } }
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
                            ListBottomSheet(
                                title = context.getString(R.string.choose_quality_for, model.name),
                                list = c,
                                onClick = { onAction(it) }
                            ) {
                                ListBottomSheetItemModel(
                                    primaryText = it.quality.orEmpty(),
                                    icon = when (getQualityFromName(it.quality.orEmpty())) {
                                        Qualities.Unknown -> Icons.Default.DeviceUnknown
                                        Qualities.P360 -> Icons.Default._360
                                        Qualities.P480 -> Icons.Default._4mp
                                        Qualities.P720 -> Icons.Default._7mp
                                        Qualities.P1080 -> Icons.Default._10mp
                                        Qualities.P1440 -> Icons.Default._1k
                                        Qualities.P2160 -> Icons.Default._4k
                                        else -> Icons.Default.DeviceUnknown
                                    }
                                )
                            }.show(activity.supportFragmentManager, "qualityChooser")
                        }
                        else -> {
                            Toast.makeText(context, context.getString(errorId, model.source.serviceName), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    override fun sourceList(): List<ApiService> = Sources.values().filterNot(Sources::notWorking).toList()

    override fun searchList(): List<ApiService> = Sources.searchSources

    override fun toSource(s: String): ApiService? = try {
        Sources.valueOf(s)
    } catch (e: IllegalArgumentException) {
        null
    }

    @Composable
    override fun DetailActions(infoModel: InfoModel, tint: Color) {
        val showCast by MainActivity.cast.sessionConnected().collectAsState(initial = true)

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

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    override fun ComposeShimmerItem() {
        LazyColumn {
            items(10) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .placeholder(
                                true,
                                highlight = PlaceholderHighlight.shimmer(),
                                color = androidx.compose.material3
                                    .contentColorFor(backgroundColor = MaterialTheme.colorScheme.surface)
                                    .copy(0.1f)
                                    .compositeOver(MaterialTheme.colorScheme.surface)
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
                                .padding(5.dp)
                        )
                    }
                }
            }
        }
    }

    @OptIn(
        ExperimentalMaterialApi::class,
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class,
    )
    @Composable
    override fun ItemListView(
        list: List<ItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (ItemModel, ComponentState) -> Unit,
        onClick: (ItemModel) -> Unit
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(list) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp)
                        .combineClickableWithIndication(
                            onLongPress = { c -> onLongPress(it, c) },
                            onClick = { onClick(it) }
                        )
                ) {
                    ListItem(
                        leadingContent = {
                            androidx.compose.material3.Icon(
                                if (favorites.fastAny { f -> f.url == it.url }) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                            )
                        },
                        headlineText = { androidx.compose.material3.Text(it.title) },
                        overlineText = { androidx.compose.material3.Text(it.source.serviceName) },
                        supportingText = if (it.description.isNotEmpty()) {
                            { androidx.compose.material3.Text(it.description) }
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

    @OptIn(ExperimentalMaterialApi::class)
    override fun composeCustomPreferences(navController: NavController): ComposeSettingsDsl.() -> Unit = {

        viewSettings {
            val context = LocalContext.current

            PreferenceSetting(
                settingTitle = { androidx.compose.material3.Text(stringResource(R.string.video_menu_title)) },
                settingIcon = { androidx.compose.material3.Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { navController.navigate(ViewVideoViewModel.VideoViewerRoute) { launchSingleTop = true } }
            )

            val castingViewModel: CastingViewModel = viewModel()
            val activity = LocalActivity.current

            ShowWhen(castingViewModel.connection) {
                PreferenceSetting(
                    settingTitle = { androidx.compose.material3.Text(stringResource(R.string.cast_menu_title)) },
                    settingIcon = {
                        androidx.compose.material3.Icon(
                            if (castingViewModel.session) Icons.Default.CastConnected else Icons.Default.Cast,
                            null,
                            modifier = Modifier.fillMaxSize()
                        )
                    },
                    modifier = Modifier.clickable(
                        indication = rememberRipple(),
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        if (MainActivity.cast.isCastActive()) {
                            context.startActivity(Intent(context, ExpandedControlsActivity::class.java))
                        } else {
                            MediaRouteDialogFactory.getDefault().onCreateChooserDialogFragment()
                                .also { m ->
                                    CastContext.getSharedInstance(context) {}.result.mergedSelector?.let { m.routeSelector = it }
                                }
                                .show(activity.supportFragmentManager, "media_chooser")
                        }
                    }
                )
            }

            PreferenceSetting(
                settingTitle = { androidx.compose.material3.Text(stringResource(R.string.downloads_menu_title)) },
                settingIcon = { androidx.compose.material3.Icon(Icons.Default.Download, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { navController.navigate(DownloaderViewModel.DownloadViewerRoute) { launchSingleTop = true } }
            )
        }

        generalSettings {
            val context = LocalContext.current
            var folderLocation by remember { mutableStateOf(context.folderLocation) }
            val activity = LocalActivity.current

            PreferenceSetting(
                settingTitle = { androidx.compose.material3.Text(stringResource(R.string.folder_location)) },
                summaryValue = { androidx.compose.material3.Text(folderLocation) },
                settingIcon = { androidx.compose.material3.Icon(Icons.Default.Folder, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    activity.requestPermissions(
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

        playerSettings {

            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            val player by context.useNewPlayerFlow.collectAsState(true)

            SwitchSetting(
                settingTitle = { androidx.compose.material3.Text(stringResource(R.string.use_new_player)) },
                summaryValue = { androidx.compose.material3.Text(stringResource(R.string.use_new_player_description)) },
                settingIcon = { androidx.compose.material3.Icon(Icons.Default.PersonalVideo, null, modifier = Modifier.fillMaxSize()) },
                value = player,
                updateValue = { scope.launch { context.updatePref(USER_NEW_PLAYER, it) } }
            )

            val ignoreSsl by context.ignoreSsl.collectAsState(initial = true)

            SwitchSetting(
                settingTitle = { androidx.compose.material3.Text(stringResource(id = R.string.ignore_ssl)) },
                settingIcon = { androidx.compose.material3.Icon(Icons.Default.Security, null, modifier = Modifier.fillMaxSize()) },
                value = ignoreSsl
            ) { scope.launch { context.updatePref(IGNORE_SSL, it) } }

        }

    }

    override fun settingNavSetup(fragment: Fragment, navController: NavController) {
        /*navController
            .graph
            .addDestination(
                FragmentNavigator(fragment.requireContext(), fragment.childFragmentManager, R.id.setting_nav).createDestination().apply {
                    id = ViewVideosFragment::class.java.hashCode()
                    setClassName(ViewVideosFragment::class.java.name)
                    addDeepLink(MainActivity.VIEW_VIDEOS)
                }
            )

        navController
            .graph
            .addDestination(
                FragmentNavigator(fragment.requireContext(), fragment.childFragmentManager, R.id.setting_nav).createDestination().apply {
                    id = DownloadViewerFragment::class.java.hashCode()
                    setClassName(DownloadViewerFragment::class.java.name)
                    addDeepLink(MainActivity.VIEW_DOWNLOADS)
                }
            )*/
    }

    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalMaterialApi::class
    )
    override fun NavGraphBuilder.navSetup() {

        composable(
            ViewVideoViewModel.VideoViewerRoute,
            enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Down) },
            deepLinks = listOf(navDeepLink { uriPattern = "animeworld://${ViewVideoViewModel.VideoViewerRoute}" })
        ) { ViewVideoScreen() }

        composable(
            DownloaderViewModel.DownloadViewerRoute,
            enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Down) },
            deepLinks = listOf(navDeepLink { uriPattern = "animeworld://${DownloaderViewModel.DownloadViewerRoute}" })
        ) { DownloaderUi() }

        composable(
            VideoViewModel.VideoPlayerRoute,
            enterTransition = { slideIntoContainer(AnimatedContentScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentScope.SlideDirection.Down) },
        ) { VideoPlayerUi() }

    }

    override fun deepLinkDetails(context: Context, itemModel: ItemModel?): PendingIntent? {
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
