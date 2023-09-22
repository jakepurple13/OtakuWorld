package com.programmersbox.animeworld

import android.Manifest
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.ProgressBar
import android.widget.Toast
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.lifecycleScope
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
import com.programmersbox.animeworld.videochoice.VideoSourceViewModel
import com.programmersbox.animeworld.videoplayer.VideoPlayerUi
import com.programmersbox.animeworld.videoplayer.VideoViewModel
import com.programmersbox.animeworld.videos.ViewVideoScreen
import com.programmersbox.animeworld.videos.ViewVideoViewModel
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.helpfulutils.downloadManager
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.models.Storage
import com.programmersbox.sharedutils.AppUpdate
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.settings.ComposeSettingsDsl
import com.programmersbox.uiviews.utils.ComponentState
import com.programmersbox.uiviews.utils.LocalActivity
import com.programmersbox.uiviews.utils.LocalNavController
import com.programmersbox.uiviews.utils.NotificationLogo
import com.programmersbox.uiviews.utils.PreferenceSetting
import com.programmersbox.uiviews.utils.ShowWhen
import com.programmersbox.uiviews.utils.SwitchSetting
import com.programmersbox.uiviews.utils.bottomSheet
import com.programmersbox.uiviews.utils.combineClickableWithIndication
import com.programmersbox.uiviews.utils.components.placeholder.PlaceholderHighlight
import com.programmersbox.uiviews.utils.components.placeholder.m3placeholder
import com.programmersbox.uiviews.utils.components.placeholder.shimmer
import com.programmersbox.uiviews.utils.updatePref
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.koin.dsl.module

val appModule = module {
    single<GenericInfo> { GenericAnime(get(), get()) }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
    single { StorageHolder() }
}

class StorageHolder {
    var storageModel: Storage? = null
}

class GenericAnime(
    val context: Context,
    val storageHolder: StorageHolder,
) : GenericInfo {

    override val apkString: AppUpdate.AppUpdates.() -> String? get() = { if (BuildConfig.FLAVOR == "noFirebase") anime_no_firebase_file else anime_file }
    override val deepLinkUri: String get() = "animeworld://"

    override val sourceType: String get() = "anime"

    override fun chapterOnClick(
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController,
    ) {
        /*if ((model.source as? ShowApi)?.canPlay == false) {
            Toast.makeText(context, context.getString(R.string.source_no_stream, model.source.serviceName), Toast.LENGTH_SHORT).show()
            return
        }*/
        getEpisodes(
            R.string.source_no_stream,
            model,
            infoModel,
            context,
            activity,
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
        model: ChapterModel,
        allChapters: List<ChapterModel>,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        navController: NavController
    ) {
        /* if ((model.source as? ShowApi)?.canDownload == false) {
             Toast.makeText(
                 context,
                 context.getString(R.string.source_no_download, model.source.serviceName),
                 Toast.LENGTH_SHORT
             ).show()
             return
         }*/
        activity.requestPermissions(
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
                    R.string.source_no_download,
                    model,
                    infoModel,
                    context,
                    activity,
                    { !it.link.orEmpty().endsWith(".m3u8") },
                    navController,
                    false
                ) {
                    //fetchIt(it, model, activity)
                    downloadVideo(activity, model, it)
                }
            }
        }
    }

    private fun getEpisodes(
        errorId: Int,
        model: ChapterModel,
        infoModel: InfoModel,
        context: Context,
        activity: FragmentActivity,
        filter: (Storage) -> Boolean = { true },
        navController: NavController,
        isStreaming: Boolean,
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
                            navController.navigate(VideoSourceViewModel.createNavigationRoute(c, infoModel, isStreaming, model))
                        }

                        else -> {
                            Toast.makeText(context, context.getString(errorId, model.source.serviceName), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    fun downloadVideo(context: Context, model: ChapterModel, storage: Storage) {
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

    override fun sourceList(): List<ApiService> = emptyList()

    override fun searchList(): List<ApiService> = emptyList()

    override fun toSource(s: String): ApiService? = null

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
            val context = LocalContext.current
            val navController = LocalNavController.current

            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.video_menu_title)) },
                settingIcon = { Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.fillMaxSize()) },
                modifier = Modifier.clickable(
                    indication = rememberRipple(),
                    interactionSource = remember { MutableInteractionSource() }
                ) { navController.navigate(ViewVideoViewModel.VideoViewerRoute) { launchSingleTop = true } }
            )

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
        }

        generalSettings {
            val context = LocalContext.current
            var folderLocation by remember { mutableStateOf(context.folderLocation) }
            val activity = LocalActivity.current

            PreferenceSetting(
                settingTitle = { Text(stringResource(R.string.folder_location)) },
                summaryValue = { Text(folderLocation) },
                settingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.fillMaxSize()) },
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
                settingTitle = { Text(stringResource(R.string.use_new_player)) },
                summaryValue = { Text(stringResource(R.string.use_new_player_description)) },
                settingIcon = { Icon(Icons.Default.PersonalVideo, null, modifier = Modifier.fillMaxSize()) },
                value = player,
                updateValue = { scope.launch { context.updatePref(USER_NEW_PLAYER, it) } }
            )

            val ignoreSsl by context.ignoreSsl.collectAsState(initial = true)

            SwitchSetting(
                settingTitle = { Text(stringResource(id = R.string.ignore_ssl)) },
                settingIcon = { Icon(Icons.Default.Security, null, modifier = Modifier.fillMaxSize()) },
                value = ignoreSsl
            ) { scope.launch { context.updatePref(IGNORE_SSL, it) } }
        }
    }

    override fun NavGraphBuilder.globalNavSetup() {
        composable(
            VideoViewModel.VideoPlayerRoute,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
        ) { VideoPlayerUi() }

        bottomSheet(VideoSourceViewModel.route) { VideoChoiceScreen() }
    }

    override fun NavGraphBuilder.settingsNavSetup() {
        composable(
            ViewVideoViewModel.VideoViewerRoute,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) },
            deepLinks = listOf(navDeepLink { uriPattern = "animeworld://${ViewVideoViewModel.VideoViewerRoute}" })
        ) { ViewVideoScreen() }
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
