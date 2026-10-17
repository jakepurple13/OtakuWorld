package com.programmersbox.animeworld

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PersonalVideo
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.app.MediaRouteDialogFactory
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.google.android.gms.cast.framework.CastContext
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.obsez.android.lib.filechooser.ChooserDialog
import com.programmersbox.anime.shared.GenericSharedAnime
import com.programmersbox.anime.shared.StorageHolder
import com.programmersbox.anime.shared.downloads.EpisodeDownloadRequest
import com.programmersbox.anime.shared.downloads.enqueueEpisodeDownload
import com.programmersbox.anime.shared.videoplayer.playerHeaders
import com.programmersbox.anime.shared.subtitles.DataStoreSubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRepository
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsRoute
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsScreen
import com.programmersbox.anime.shared.subtitles.SubtitleSettingsViewModel
import com.programmersbox.anime.shared.VideoScreen
import com.programmersbox.anime.shared.videochoice.VideoChoiceScreen
import com.programmersbox.anime.shared.videochoice.VideoSourceModel
import com.programmersbox.anime.shared.videoplayer.VideoPlayerUi
import com.programmersbox.anime.shared.videos.SharedVideoContent
import com.programmersbox.anime.shared.videos.VideoLibrarySource
import com.programmersbox.anime.shared.videos.VideoViewerRoute
import com.programmersbox.anime.shared.videos.ViewVideoScreen
import com.programmersbox.animeworld.cast.ExpandedControlsActivity
import com.programmersbox.datastore.asState
import com.programmersbox.helpfulutils.requestPermissions
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.aniyomi.aniyomiModule
import com.programmersbox.cloudstream.cloudstreamModule
import com.programmersbox.kmpuiviews.SystemAlerter
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.components.settings.CategoryGroup
import com.programmersbox.kmpuiviews.presentation.components.settings.PreferenceSetting
import com.programmersbox.kmpuiviews.presentation.components.settings.ShowWhen
import com.programmersbox.kmpuiviews.presentation.components.settings.SwitchSetting
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.Backup
import com.programmersbox.kmpuiviews.utils.ComposeSettingsDsl
import com.programmersbox.kmpuiviews.utils.LocalNavActions
import com.programmersbox.kmpuiviews.utils.NotificationLogo
import com.programmersbox.kmpuiviews.utils.Zipper
import com.programmersbox.sharedtools.BackupProcessor
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.bindsGenericInfo
import kotlinx.coroutines.CancellationException
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
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import java.io.File

val appModule = module {
    singleOf(::GenericAnime) { bindsGenericInfo() }
    single { NotificationLogo(R.mipmap.ic_launcher_foreground) }
    single { StorageHolder() }
    single<SubtitleSettingsRepository> { DataStoreSubtitleSettingsRepository(get<Context>()) }
    viewModelOf(::SubtitleSettingsViewModel)
    singleOf(::VideoLibrarySource)
    single { AnimeDataStoreHandling() }
    singleOf(::Backup)
    factory { Zipper(get(), getAll<BackupProcessor>(), get()) }
    single { SystemAlerter(get(), get(), BuildConfig.APPLICATION_ID) }

    navigation<VideoScreen> { VideoPlayerUi(it) }

    navigation<SubtitleSettingsRoute> { SubtitleSettingsScreen() }

    navigation<Screen.SourcePreferences> { SourcePreferencesRoute(it.serviceName) }

    includes(aniyomiModule)
    includes(cloudstreamModule)

    navigation<VideoViewerRoute> {
        val context = LocalContext.current
        ViewVideoScreen(
            isCastActive = { MainActivity.cast.isCastActive() },
            onCastLoad = { content: SharedVideoContent ->
                MainActivity.cast.loadMedia(
                    File(content.path),
                    context.getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(content.path, 0L),
                    null, null
                )
            },
            castButton = {
                AndroidView(
                    factory = { ctx ->
                        MediaRouteButton(ctx).apply {
                            MainActivity.cast.showIntroductoryOverlay(this)
                            MainActivity.cast.setMediaRouteMenu(ctx, this)
                        }
                    }
                )
            },
            deleteDialog = { content, onResult ->
                AlertDialog(
                    onDismissRequest = { onResult(false) },
                    title = { Text(stringResource(R.string.remove)) },
                    text = { Text(content.videoName) },
                    confirmButton = {
                        TextButton(onClick = { onResult(true) }) { Text(stringResource(R.string.remove)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { onResult(false) }) { Text(stringResource(R.string.cancelText)) }
                    }
                )
            }
        )
    }
}

class GenericAnime(
    val context: Context,
    val storageHolder: StorageHolder,
    val animeDataStoreHandling: AnimeDataStoreHandling,
    appConfig: AppConfig,
) : GenericSharedAnime(appConfig = appConfig), GenericInfo {

    override val deepLinkUri: String get() = "animeworld://"

    override fun onResolveFailed(storage: KmpStorage, error: Throwable) {
        val name = listOfNotNull(storage.source, storage.quality).joinToString(" ").ifBlank { "stream" }
        runOnUIThread {
            Toast.makeText(context, "Couldn't load $name: ${error.message ?: error::class.simpleName}", Toast.LENGTH_LONG).show()
        }
    }

    override fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
    ) {
        if (MainActivity.cast.isCastActive() && model.source.canCast) {
            MainActivity.cast.loadUrl(
                storage.link,
                infoModel.title,
                model.name,
                infoModel.imageUrl,
                storage.headers
            )
        } else {
            storageHolder.storageModel = storage
            navController.navigate(
                VideoScreen(
                    showPath = storage.link.orEmpty(),
                    showName = model.name,
                    downloadOrStream = false,
                    referer = storage.headers["referer"] ?: storage.source.orEmpty()
                )
            )
        }
    }

    override fun downloadChapter(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        getEpisodes(
            errorId = R.string.source_no_download,
            model = model,
            infoModel = infoModel,
            context = context,
            isStreaming = false
        ) {
            CoroutineScope(Job() + Dispatchers.IO).launch { downloadResolved(model, infoModel, it) }
        }
    }

    /** Lets the source resolve a lazily-described stream first, then hands it to the episode downloader. */
    suspend fun downloadResolved(model: KmpChapterModel, infoModel: KmpInfoModel, storage: KmpStorage) {
        val resolved = try {
            withContext(Dispatchers.IO) { model.source.resolve(storage) }.also { r ->
                if (r !== storage) {
                    if (r.headers.isEmpty()) r.headers.putAll(storage.headers)
                    if (r.extras.isEmpty()) r.extras.putAll(storage.extras)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            onResolveFailed(storage, e)
            return
        }
        val link = resolved.link
        if (link.isNullOrBlank()) {
            onResolveFailed(storage, IllegalStateException("No link"))
            return
        }
        enqueueEpisodeDownload(
            context,
            EpisodeDownloadRequest(
                url = link,
                headers = playerHeaders(resolved.headers, resolved.headers["referer"] ?: resolved.source),
                mimeType = resolved.mimeType,
                qualityHint = resolved.quality,
                subtitles = resolved.subtitles,
                title = "${infoModel.title} - ${model.name}",
                folder = context.folderLocation,
            )
        )
        withContext(Dispatchers.Main) {
            Toast.makeText(context, R.string.downloading_dots_no_percent, Toast.LENGTH_SHORT).show()
        }
    }

    private fun getEpisodes(
        errorId: Int,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        context: Context,
        filter: (KmpStorage) -> Boolean = { true },
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
                val navController = LocalNavActions.current
                PreferenceSetting(
                    settingTitle = { Text(stringResource(R.string.video_menu_title)) },
                    settingIcon = { Icon(Icons.Default.VideoLibrary, null, modifier = Modifier.fillMaxSize()) },
                    modifier = Modifier.clickable(
                        indication = ripple(),
                        interactionSource = null
                    ) { navController.navigate(VideoViewerRoute) }
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
                    val navController = LocalNavActions.current
                    PreferenceSetting(
                        settingTitle = { Text(stringResource(R.string.subtitles)) },
                        settingIcon = { Icon(Icons.Default.Subtitles, null, modifier = Modifier.fillMaxSize()) },
                        modifier = Modifier.clickable(
                            indication = ripple(),
                            interactionSource = null
                        ) { navController.navigate(SubtitleSettingsRoute) }
                    )
                }

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

    context(navGraph: EntryProviderScope<NavKey>)
    override fun globalNav3Setup() {
        navGraph.entry<VideoScreen> {
            VideoPlayerUi(it)
        }
    }

    context(navGraph: EntryProviderScope<NavKey>)
    override fun settingsNav3Setup() {
        navGraph.entry<VideoViewerRoute> {
            ViewVideoScreen(
                isCastActive = { MainActivity.cast.isCastActive() },
                onCastLoad = { content: SharedVideoContent ->
                    MainActivity.cast.loadMedia(
                        File(content.path),
                        context.getSharedPreferences("videos", Context.MODE_PRIVATE).getLong(content.path, 0L),
                        null, null
                    )
                },
                castButton = {
                    AndroidView(
                        factory = { ctx ->
                            MediaRouteButton(ctx).apply {
                                MainActivity.cast.showIntroductoryOverlay(this)
                                MainActivity.cast.setMediaRouteMenu(ctx, this)
                            }
                        }
                    )
                },
                deleteDialog = { content, onResult ->
                    AlertDialog(
                        onDismissRequest = { onResult(false) },
                        title = { Text(stringResource(R.string.remove)) },
                        text = { Text(content.videoName) },
                        confirmButton = {
                            TextButton(onClick = { onResult(true) }) { Text(stringResource(R.string.remove)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { onResult(false) }) { Text(stringResource(R.string.cancelText)) }
                        }
                    )
                }
            )
        }
    }

    @Composable
    override fun DialogSetups() {
        VideoSourceModel.showVideoSources?.let {
            val navController = LocalNavActions.current
            VideoChoiceScreen(
                items = it.c,
                infoModel = it.infoModel,
                model = it.model,
                genericInfo = this,
                navController = navController,
                onPlay = { s ->
                    if (it.isStreaming) playResolved(navController, s, it.model, it.infoModel)
                    else downloadResolved(it.model, it.infoModel, s)
                }
            )
        }
    }

    override fun deepLinkDetails(context: Context, itemModel: KmpItemModel?): PendingIntent? {
        return deepLinkSetup(
            context = context,
            uri = deepLinkDetailsUri(itemModel),
            activity = MainActivity::class.java
        )
    }

    override fun deepLinkSettings(context: Context): PendingIntent? {
        return deepLinkSetup(
            context = context,
            uri = deepLinkSettingsUri(),
            activity = MainActivity::class.java
        )
    }
}
